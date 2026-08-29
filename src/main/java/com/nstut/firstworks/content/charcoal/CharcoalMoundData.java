package com.nstut.firstworks.content.charcoal;

import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One saved controller record per physical charcoal mound. The logs and earth
 * remain ordinary world blocks; no log or sealant receives a block entity.
 */
public final class CharcoalMoundData extends SavedData {
    private static final String DATA_NAME = "firstworks_charcoal_mounds";

    private final List<Charge> charges = new ArrayList<>();
    private final Map<BlockPos, Charge> chargeByLog = new HashMap<>();

    public static CharcoalMoundData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CharcoalMoundData::new, CharcoalMoundData::load), DATA_NAME);
    }

    public record ShellCoverage(int exteriorFaces, int sealedFaces) {
        public float ratio() {
            return exteriorFaces > 0 ? (float) sealedFaces / (float) exteriorFaces : 0.0F;
        }
    }

    public static ShellCoverage calculateShellCoverage(ServerLevel level, Set<BlockPos> logs) {
        int exterior = 0;
        int sealed = 0;
        for (BlockPos log : logs) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = log.relative(dir);
                if (logs.contains(neighbor)) continue;
                exterior++;
                if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(ModTags.CHARCOAL_SEALANTS)) {
                    sealed++;
                }
            }
        }
        return new ShellCoverage(exterior, sealed);
    }

    public record IgnitionProbe(
            boolean isMoundCandidate,
            boolean isValid,
            @Nullable Component failureReason,
            Set<BlockPos> logs,
            BlockPos opening
    ) {}

    public IgnitionProbe probe(ServerLevel level, BlockPos ignitionLog, Direction exposedFace) {
        if (!level.getBlockState(ignitionLog).is(ModTags.CHARCOAL_WOODS)) {
            return new IgnitionProbe(false, false, null, Set.of(), ignitionLog);
        }

        Set<BlockPos> logs = discoverLogs(level, ignitionLog);
        BlockPos opening = ignitionLog.relative(exposedFace);

        // A deliberate charcoal mound has substantial shell coverage (>= 50% exterior faces sealed)
        // or directly overlaps an existing active charge. Ordinary standing trees have <= 10% coverage.
        ShellCoverage coverage = calculateShellCoverage(level, logs);
        boolean isMoundCandidate = charges.stream().anyMatch(charge -> charge.overlaps(logs))
                || coverage.ratio() >= 0.50F;

        if (!isMoundCandidate) {
            return new IgnitionProbe(false, false, null, logs, opening);
        }

        int minLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MIN_LOGS.get();
        int maxLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();

        if (logs.size() < minLogs) {
            return new IgnitionProbe(true, false,
                    Component.translatable("message.firstworks.charcoal.too_small", minLogs), logs, opening);
        }
        if (logs.size() > maxLogs) {
            return new IgnitionProbe(true, false,
                    Component.translatable("message.firstworks.charcoal.too_large", maxLogs), logs, opening);
        }
        if (charges.stream().anyMatch(charge -> charge.overlaps(logs))) {
            return new IgnitionProbe(true, false,
                    Component.translatable("message.firstworks.charcoal.already_lit"), logs, opening);
        }
        if (logs.contains(opening) || level.getBlockState(opening).is(ModTags.CHARCOAL_SEALANTS)) {
            return new IgnitionProbe(true, false,
                    Component.translatable("message.firstworks.charcoal.need_opening"), logs, opening);
        }
        if (!isShellValid(level, logs, opening, true)) {
            return new IgnitionProbe(true, false,
                    Component.translatable("message.firstworks.charcoal.unsealed"), logs, opening);
        }

        return new IgnitionProbe(true, true, null, logs, opening);
    }

    public IgnitionResult igniteFromProbe(ServerLevel level, IgnitionProbe probe) {
        if (!probe.isValid()) {
            return IgnitionResult.failure(probe.failureReason());
        }

        int sealWindow = com.nstut.firstworks.FirstworksConfig.CHARCOAL_SEAL_WINDOW.get();
        Charge charge = new Charge(probe.logs(), probe.opening(), Phase.WAITING_FOR_SEAL,
                level.getGameTime() + sealWindow);
        charges.add(charge);
        for (BlockPos log : charge.logs) {
            chargeByLog.put(log, charge);
        }
        setDirty();

        // Diegetic feedback: firecharge sound + visible flame and smoke burst from opening
        level.playSound(null, probe.opening(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.85F, 0.7F);
        level.playSound(null, probe.opening(), SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.7F, 0.9F);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                probe.opening().getX() + 0.5, probe.opening().getY() + 0.5, probe.opening().getZ() + 0.5,
                10, 0.15, 0.15, 0.15, 0.02);
        level.sendParticles(ParticleTypes.FLAME,
                probe.opening().getX() + 0.5, probe.opening().getY() + 0.35, probe.opening().getZ() + 0.5,
                6, 0.12, 0.12, 0.12, 0.015);

        return IgnitionResult.success();
    }

    public IgnitionResult ignite(ServerLevel level, BlockPos ignitionLog, Direction exposedFace) {
        IgnitionProbe probe = probe(level, ignitionLog, exposedFace);
        return igniteFromProbe(level, probe);
    }

    private Charge findChargeAt(BlockPos pos) {
        Charge direct = chargeByLog.get(pos);
        if (direct != null) return direct;
        for (Direction direction : Direction.values()) {
            Charge neighbor = chargeByLog.get(pos.relative(direction));
            if (neighbor != null) return neighbor;
        }
        return null;
    }

    public Optional<MoundStatus> getStatusAt(BlockPos pos, long gameTime) {
        Charge charge = findChargeAt(pos);
        if (charge == null) return Optional.empty();
        long remainingTicks = Math.max(0L, charge.deadline - gameTime);
        float normalYield = com.nstut.firstworks.FirstworksConfig.CHARCOAL_NORMAL_YIELD.get().floatValue();
        int expectedYield = Mth.floor(charge.logs.size() * normalYield);
        return Optional.of(new MoundStatus(charge.phase, charge.logs.size(), remainingTicks, expectedYield));
    }

    public Optional<MoundStatus> getStatusAt(ServerLevel level, BlockPos pos) {
        return getStatusAt(pos, level.getGameTime());
    }

    public void onBlockPlaced(ServerLevel level, BlockPos pos) {
        if (charges.isEmpty()) return;
        boolean changed = false;
        long time = level.getGameTime();
        for (Charge charge : charges) {
            if (charge.phase == Phase.WAITING_FOR_SEAL && charge.opening.equals(pos)) {
                charge.pendingSeal = pos.immutable();
                charge.pendingSealTime = time;
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    public void tick(ServerLevel level) {
        if (charges.isEmpty()) return;
        boolean isPeriodicTick = (level.getGameTime() % 20L == 0L);
        boolean changed = false;
        float normalYield = com.nstut.firstworks.FirstworksConfig.CHARCOAL_NORMAL_YIELD.get().floatValue();
        float breachedYield = com.nstut.firstworks.FirstworksConfig.CHARCOAL_BREACHED_YIELD.get().floatValue();
        int carbonizationTicks = com.nstut.firstworks.FirstworksConfig.CHARCOAL_CARBONIZE_DURATION.get();

        Iterator<Charge> iterator = charges.iterator();
        while (iterator.hasNext()) {
            Charge charge = iterator.next();

            boolean needsWork = (charge.pendingBreach != null || charge.pendingSeal != null || isPeriodicTick);
            if (!needsWork) continue;

            // Ensure chunk is loaded before accessing world blocks or migrating legacy charges
            if (!charge.isLoaded(level)) continue;

            // 1. Backward compatibility: migrate legacy READY charges once loaded
            if (charge.phase == Phase.LEGACY_READY) {
                materializeCharcoal(level, charge, normalYield);
                removeChargeFromIndex(charge);
                iterator.remove();
                changed = true;
                continue;
            }

            // 2. Evaluate deferred 1-tick pending seal (immune to cancellation races)
            if (charge.pendingSeal != null && charge.phase == Phase.WAITING_FOR_SEAL) {
                BlockPos sealPos = charge.pendingSeal;
                long sealTime = charge.pendingSealTime;
                charge.pendingSeal = null;
                changed = true;

                if (sealTime <= charge.deadline && level.getBlockState(sealPos).is(ModTags.CHARCOAL_SEALANTS)) {
                    charge.phase = Phase.CARBONIZING;
                    charge.deadline = level.getGameTime() + carbonizationTicks;
                    level.playSound(null, charge.opening, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                            SoundSource.BLOCKS, 0.75F, 0.6F);
                    level.playSound(null, charge.opening, SoundEvents.GRAVEL_PLACE,
                            SoundSource.BLOCKS, 0.8F, 0.65F);
                } else if (level.getGameTime() >= charge.deadline) {
                    removeChargeFromIndex(charge);
                    iterator.remove();
                    continue;
                }
            }

            // 3. Evaluate event-driven pending breach bound to this specific charge
            if (charge.pendingBreach != null) {
                BlockPos breachPos = charge.pendingBreach;
                long breachTime = charge.pendingBreachTime;
                boolean isLog = charge.logs.contains(breachPos);
                boolean blockActuallyDestroyed = isLog
                        ? !level.getBlockState(breachPos).is(ModTags.CHARCOAL_WOODS)
                        : !level.getBlockState(breachPos).is(ModTags.CHARCOAL_SEALANTS);

                if (blockActuallyDestroyed) {
                    if (charge.phase == Phase.WAITING_FOR_SEAL) {
                        if (isLog || !breachPos.equals(charge.opening)) {
                            removeChargeFromIndex(charge);
                            iterator.remove();
                            changed = true;
                            continue;
                        }
                    } else if (charge.phase == Phase.CARBONIZING) {
                        // Dramatic breach feedback
                        level.playSound(null, breachPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.5F);
                        level.playSound(null, breachPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.8F, 0.6F);
                        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                                breachPos.getX() + 0.5, breachPos.getY() + 0.8, breachPos.getZ() + 0.5,
                                12, 0.25, 0.25, 0.25, 0.04);
                        level.sendParticles(ParticleTypes.SMOKE,
                                breachPos.getX() + 0.5, breachPos.getY() + 0.5, breachPos.getZ() + 0.5,
                                16, 0.35, 0.35, 0.35, 0.02);

                        if (breachTime >= charge.deadline) {
                            materializeCharcoal(level, charge, normalYield);
                        } else {
                            materializeCharcoal(level, charge, breachedYield);
                        }
                        removeChargeFromIndex(charge);
                        iterator.remove();
                        changed = true;
                        continue;
                    }
                } else {
                    charge.pendingBreach = null;
                    changed = true;
                }
            }

            // 4. Periodic integrity and deadline checks
            if (!isPeriodicTick) continue;

            if (charge.phase == Phase.WAITING_FOR_SEAL) {
                if (level.getGameTime() >= charge.deadline) {
                    removeChargeFromIndex(charge);
                    iterator.remove();
                    changed = true;
                    continue;
                }
                if (!charge.logsIntact(level)
                        || !isShellValid(level, charge.logs, charge.opening, true)) {
                    removeChargeFromIndex(charge);
                    iterator.remove();
                    changed = true;
                    continue;
                }
                if (level.getBlockState(charge.opening).is(ModTags.CHARCOAL_SEALANTS)) {
                    charge.phase = Phase.CARBONIZING;
                    charge.deadline = level.getGameTime() + carbonizationTicks;
                    level.playSound(null, charge.opening, SoundEvents.GENERIC_EXTINGUISH_FIRE,
                            SoundSource.BLOCKS, 0.75F, 0.6F);
                    level.playSound(null, charge.opening, SoundEvents.GRAVEL_PLACE,
                            SoundSource.BLOCKS, 0.8F, 0.65F);
                    changed = true;
                } else {
                    // Active fire and smoke escaping from opening while waiting for seal
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            charge.opening.getX() + 0.5, charge.opening.getY() + 0.6, charge.opening.getZ() + 0.5,
                            3, 0.12, 0.1, 0.12, 0.01);
                    level.sendParticles(ParticleTypes.SMALL_FLAME,
                            charge.opening.getX() + 0.5, charge.opening.getY() + 0.35, charge.opening.getZ() + 0.5,
                            1, 0.08, 0.08, 0.08, 0.005);
                    if (level.getGameTime() % 40L == 0L) {
                        level.playSound(null, charge.opening, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.35F, 0.9F);
                    }
                }
                continue;
            }

            if (charge.phase == Phase.CARBONIZING) {
                if (level.getGameTime() >= charge.deadline) {
                    materializeCharcoal(level, charge, normalYield);
                    removeChargeFromIndex(charge);
                    iterator.remove();
                    changed = true;
                } else if (!charge.logsIntact(level) || !isShellValid(level, charge.logs, charge.opening, false)) {
                    materializeCharcoal(level, charge, breachedYield);
                    removeChargeFromIndex(charge);
                    iterator.remove();
                    changed = true;
                } else {
                    // Capped smoke points scaling (1 to 3 sources maximum)
                    int smokePoints = Math.min(3, 1 + charge.logs.size() / 32);
                    List<BlockPos> logList = new ArrayList<>(charge.logs);
                    for (int i = 0; i < smokePoints; i++) {
                        BlockPos log = logList.get(level.random.nextInt(logList.size()));
                        BlockPos surface = log.above();
                        while (charge.logs.contains(surface)) surface = surface.above();
                        level.sendParticles(ParticleTypes.SMOKE,
                                surface.getX() + 0.2 + level.random.nextDouble() * 0.6,
                                surface.getY() + 1.02,
                                surface.getZ() + 0.2 + level.random.nextDouble() * 0.6,
                                1, 0.02, 0.02, 0.02, 0.005);
                    }
                    if (level.getGameTime() % 60L == 0L) {
                        level.playSound(null, charge.opening, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.12F, 0.6F);
                    }
                }
            }
        }
        if (changed) setDirty();
    }

    public void onBlockBroken(ServerLevel level, BlockPos pos) {
        if (charges.isEmpty()) return;
        boolean changed = false;
        long time = level.getGameTime();
        for (Charge charge : charges) {
            if (charge.containsOrNeighbors(pos)) {
                if (charge.pendingBreach == null) {
                    charge.pendingBreach = pos.immutable();
                    charge.pendingBreachTime = time;
                    changed = true;
                } else {
                    charge.pendingBreachTime = Math.min(charge.pendingBreachTime, time);
                }
            }
        }
        if (changed) setDirty();
    }

    private void removeChargeFromIndex(Charge charge) {
        for (BlockPos log : charge.logs) {
            chargeByLog.remove(log);
        }
    }

    private void rebuildLogIndex() {
        chargeByLog.clear();
        for (Charge charge : charges) {
            for (BlockPos log : charge.logs) {
                chargeByLog.put(log, charge);
            }
        }
    }

    private static Set<BlockPos> discoverLogs(ServerLevel level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        int limit = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();
        while (!queue.isEmpty() && found.size() <= limit + 1) {
            BlockPos pos = queue.removeFirst();
            if (found.contains(pos) || !level.hasChunkAt(pos)
                    || !level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS)) continue;
            found.add(pos.immutable());
            for (Direction direction : Direction.values()) queue.addLast(pos.relative(direction));
        }
        return found;
    }

    private static boolean isShellValid(ServerLevel level, Set<BlockPos> logs,
            BlockPos opening, boolean allowOpening) {
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = log.relative(direction);
                if (logs.contains(neighbor)) continue;
                if (allowOpening && neighbor.equals(opening)) continue;
                if (!level.hasChunkAt(neighbor) || !level.getBlockState(neighbor).is(ModTags.CHARCOAL_SEALANTS)) return false;
            }
        }
        return true;
    }

    private static void materializeCharcoal(ServerLevel level, Charge charge, float yield) {
        List<BlockPos> consumedPositions = new ArrayList<>();
        for (BlockPos pos : charge.logs) {
            if (!level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS)) continue;
            consumedPositions.add(pos.immutable());
        }

        int totalCharcoal = Mth.floor(consumedPositions.size() * yield);

        for (BlockPos pos : consumedPositions) {
            level.removeBlock(pos, false);
        }

        if (totalCharcoal > 0) {
            // Sort bottom-up deterministically: Y ascending, then X ascending, then Z ascending
            List<BlockPos> sortedPositions = consumedPositions.stream()
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getZ))
                    .toList();

            int remaining = totalCharcoal;
            for (BlockPos pos : sortedPositions) {
                if (remaining <= 0) break;
                int placeAmount = Math.min(remaining, 4);
                level.setBlock(pos, ModBlocks.CHARCOAL_PILE.get().defaultBlockState()
                        .setValue(CharcoalPileBlock.AMOUNT, placeAmount), 3);
                remaining -= placeAmount;

                // Ash/charcoal dust burst
                level.sendParticles(ParticleTypes.ASH,
                        pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.01);
            }
        }
        level.playSound(null, charge.opening, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.8F);
        level.playSound(null, charge.opening, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.75F);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Charge charge : charges) {
            CompoundTag entry = new CompoundTag();
            entry.putLongArray("Logs", charge.logs.stream().mapToLong(BlockPos::asLong).toArray());
            entry.putLong("Opening", charge.opening.asLong());
            entry.putString("Phase", charge.phase.name());
            entry.putLong("Deadline", charge.deadline);
            if (charge.pendingBreach != null) {
                entry.putLong("PendingBreach", charge.pendingBreach.asLong());
                entry.putLong("PendingBreachTime", charge.pendingBreachTime);
            }
            if (charge.pendingSeal != null) {
                entry.putLong("PendingSeal", charge.pendingSeal.asLong());
                entry.putLong("PendingSealTime", charge.pendingSealTime);
            }
            list.add(entry);
        }
        tag.put("Charges", list);
        return tag;
    }

    private static CharcoalMoundData load(CompoundTag tag, HolderLookup.Provider registries) {
        CharcoalMoundData data = new CharcoalMoundData();
        ListTag list = tag.getList("Charges", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Set<BlockPos> logs = new HashSet<>();
            for (long packed : entry.getLongArray("Logs")) logs.add(BlockPos.of(packed));
            int maxLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();
            if (logs.isEmpty() || logs.size() > maxLogs) continue;
            Phase phase;
            String phaseStr = entry.getString("Phase");
            if ("READY".equalsIgnoreCase(phaseStr) || "LEGACY_READY".equalsIgnoreCase(phaseStr)) {
                phase = Phase.LEGACY_READY;
            } else {
                try {
                    phase = Phase.valueOf(phaseStr);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
            }
            BlockPos pendingBreach = entry.contains("PendingBreach") ? BlockPos.of(entry.getLong("PendingBreach")) : null;
            long pendingBreachTime = entry.getLong("PendingBreachTime");
            BlockPos pendingSeal = entry.contains("PendingSeal") ? BlockPos.of(entry.getLong("PendingSeal")) : null;
            long pendingSealTime = entry.getLong("PendingSealTime");
            Charge charge = new Charge(logs, BlockPos.of(entry.getLong("Opening")),
                    phase, entry.getLong("Deadline"), pendingBreach, pendingBreachTime,
                    pendingSeal, pendingSealTime);
            data.charges.add(charge);
            for (BlockPos log : charge.logs) {
                data.chargeByLog.put(log, charge);
            }
        }
        return data;
    }

    public record IgnitionResult(boolean isSuccessful, @Nullable Component message) {
        public static IgnitionResult success() { return new IgnitionResult(true, null); }
        public static IgnitionResult failure(Component message) { return new IgnitionResult(false, message); }
    }

    public record MoundStatus(
            Phase phase,
            int logCount,
            long remainingTicks,
            int expectedYield
    ) {}

    public enum Phase { WAITING_FOR_SEAL, CARBONIZING, LEGACY_READY }

    private static final class Charge {
        private final Set<BlockPos> logs;
        private final BlockPos opening;
        private Phase phase;
        private long deadline;
        private net.minecraft.core.BlockPos pendingBreach;
        private long pendingBreachTime;
        private net.minecraft.core.BlockPos pendingSeal;
        private long pendingSealTime;

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline) {
            this(logs, opening, phase, deadline, null, 0L, null, 0L);
        }

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline,
                net.minecraft.core.BlockPos pendingBreach, long pendingBreachTime) {
            this(logs, opening, phase, deadline, pendingBreach, pendingBreachTime, null, 0L);
        }

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline,
                net.minecraft.core.BlockPos pendingBreach, long pendingBreachTime,
                net.minecraft.core.BlockPos pendingSeal, long pendingSealTime) {
            this.logs = Set.copyOf(logs);
            this.opening = opening.immutable();
            this.phase = phase;
            this.deadline = deadline;
            this.pendingBreach = pendingBreach != null ? pendingBreach.immutable() : null;
            this.pendingBreachTime = pendingBreachTime;
            this.pendingSeal = pendingSeal != null ? pendingSeal.immutable() : null;
            this.pendingSealTime = pendingSealTime;
        }

        private boolean containsOrNeighbors(BlockPos pos) {
            return logs.contains(pos) || isShellNeighbor(pos);
        }

        private boolean overlaps(Set<BlockPos> other) {
            return other.stream().anyMatch(logs::contains);
        }

        private boolean isLoaded(ServerLevel level) {
            if (!level.hasChunkAt(opening)) return false;
            for (BlockPos log : logs) {
                if (!level.hasChunkAt(log)) return false;
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = log.relative(direction);
                    if (!logs.contains(neighbor) && !level.hasChunkAt(neighbor)) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean logsIntact(ServerLevel level) {
            return logs.stream().allMatch(pos -> level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS));
        }

        private boolean isShellNeighbor(BlockPos pos) {
            if (pos.equals(opening)) return true;
            for (Direction direction : Direction.values()) {
                if (logs.contains(pos.relative(direction))) {
                    return true;
                }
            }
            return false;
        }
    }
}
