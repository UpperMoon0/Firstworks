package com.nstut.firstworks.content.charcoal;

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
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * One saved controller record per physical charcoal mound. The logs and earth
 * remain ordinary world blocks; no log or sealant receives a block entity.
 */
public final class CharcoalMoundData extends SavedData {
    private static final String DATA_NAME = "firstworks_charcoal_mounds";

    private final List<Charge> charges = new ArrayList<>();

    public static CharcoalMoundData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CharcoalMoundData::new, CharcoalMoundData::load), DATA_NAME);
    }

    public IgnitionResult ignite(ServerLevel level, BlockPos ignitionLog, Direction exposedFace) {
        Set<BlockPos> logs = discoverLogs(level, ignitionLog);
        int minLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MIN_LOGS.get();
        int maxLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();
        int sealWindow = com.nstut.firstworks.FirstworksConfig.CHARCOAL_SEAL_WINDOW.get();
        if (logs.size() < minLogs) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.too_small", minLogs));
        }
        if (logs.size() > maxLogs) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.too_large", maxLogs));
        }
        if (charges.stream().anyMatch(charge -> charge.overlaps(logs))) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.already_lit"));
        }

        BlockPos opening = ignitionLog.relative(exposedFace);
        if (logs.contains(opening) || level.getBlockState(opening).is(ModTags.CHARCOAL_SEALANTS)) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.need_opening"));
        }
        if (!isShellValid(level, logs, opening, true)) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.unsealed"));
        }

        charges.add(new Charge(logs, opening, Phase.WAITING_FOR_SEAL,
                level.getGameTime() + sealWindow));
        setDirty();
        level.playSound(null, ignitionLog, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
        return IgnitionResult.success(Component.translatable("message.firstworks.charcoal.seal_opening"));
    }

    public static boolean canIgnite(ServerLevel level, BlockPos ignitionLog, Direction exposedFace) {
        Set<BlockPos> logs = discoverLogs(level, ignitionLog);
        int minLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MIN_LOGS.get();
        int maxLogs = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();
        if (logs.size() < minLogs || logs.size() > maxLogs) return false;
        BlockPos opening = ignitionLog.relative(exposedFace);
        if (logs.contains(opening) || level.getBlockState(opening).is(ModTags.CHARCOAL_SEALANTS)) return false;
        return isShellValid(level, logs, opening, true);
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
            boolean needsWork = (charge.pendingBreach != null || isPeriodicTick);
            if (!needsWork) continue;

            if (!charge.isLoaded(level)) continue;

            // 1. Evaluate event-driven pending breach bound to this specific charge
            if (charge.pendingBreach != null) {
                BlockPos breachPos = charge.pendingBreach;
                long breachTime = charge.pendingBreachTime;
                boolean isLog = charge.logs.contains(breachPos);
                boolean blockActuallyDestroyed = isLog
                        ? !level.getBlockState(breachPos).is(ModTags.CHARCOAL_WOODS)
                        : !level.getBlockState(breachPos).is(ModTags.CHARCOAL_SEALANTS);

                if (blockActuallyDestroyed) {
                    if (charge.phase == Phase.WAITING_FOR_SEAL) {
                        if (isLog) {
                            iterator.remove();
                            changed = true;
                            continue;
                        }
                    } else if (charge.phase == Phase.CARBONIZING) {
                        if (breachTime >= charge.deadline) {
                            finish(level, charge, normalYield, breachPos);
                        } else {
                            finish(level, charge, breachedYield, breachPos);
                        }
                        iterator.remove();
                        changed = true;
                        continue;
                    } else if (charge.phase == Phase.READY) {
                        finish(level, charge, normalYield, breachPos);
                        iterator.remove();
                        changed = true;
                        continue;
                    }
                } else {
                    // Break was cancelled by another mod or didn't actually destroy the block
                    charge.pendingBreach = null;
                    changed = true;
                }
            }

            // 2. Periodic integrity and deadline checks
            if (!isPeriodicTick) continue;

            if (charge.phase == Phase.WAITING_FOR_SEAL) {
                if (level.getBlockState(charge.opening).is(ModTags.CHARCOAL_SEALANTS)) {
                    charge.phase = Phase.CARBONIZING;
                    charge.deadline = level.getGameTime() + carbonizationTicks;
                    level.playSound(null, charge.opening, SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS, 0.55F, 0.65F);
                    changed = true;
                } else if (!charge.logsIntact(level)
                        || !isShellValid(level, charge.logs, charge.opening, true)
                        || level.getGameTime() >= charge.deadline) {
                    iterator.remove();
                    changed = true;
                    continue;
                }
                smoke(level, charge.opening, 2);
                continue;
            }

            if (charge.phase == Phase.CARBONIZING) {
                if (level.getGameTime() >= charge.deadline) {
                    charge.phase = Phase.READY;
                    changed = true;
                    level.playSound(null, charge.opening, SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS, 0.7F, 0.8F);
                } else if (!charge.logsIntact(level) || !isShellValid(level, charge.logs, charge.opening, false)) {
                    finish(level, charge, breachedYield, findBreach(level, charge));
                    iterator.remove();
                    changed = true;
                } else {
                    smoke(level, charge.opening, 1);
                }
                continue;
            }

            if (charge.phase == Phase.READY) {
                if (!charge.logsIntact(level) || !isShellValid(level, charge.logs, charge.opening, false)) {
                    finish(level, charge, normalYield, findBreach(level, charge));
                    iterator.remove();
                    changed = true;
                }
            }
        }
        if (changed) setDirty();
    }

    public boolean isReadyLog(BlockPos pos) {
        for (Charge charge : charges) {
            if (charge.phase == Phase.READY && charge.logs.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    public void onReadyLogBroken(ServerLevel level, BlockPos pos) {
        if (charges.isEmpty()) return;
        Iterator<Charge> iterator = charges.iterator();
        float normalYield = com.nstut.firstworks.FirstworksConfig.CHARCOAL_NORMAL_YIELD.get().floatValue();
        while (iterator.hasNext()) {
            Charge charge = iterator.next();
            if (charge.phase == Phase.READY && charge.logs.contains(pos)) {
                if (charge.isLoaded(level)) {
                    finish(level, charge, normalYield, pos);
                    iterator.remove();
                } else {
                    charge.pendingBreach = pos.immutable();
                    charge.pendingBreachTime = level.getGameTime();
                }
                setDirty();
                return;
            }
        }
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

    private static Set<BlockPos> discoverLogs(ServerLevel level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        int limit = com.nstut.firstworks.FirstworksConfig.CHARCOAL_MAX_LOGS.get();
        while (!queue.isEmpty() && found.size() <= limit) {
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

    private static BlockPos findBreach(ServerLevel level, Charge charge) {
        for (BlockPos log : charge.logs) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = log.relative(direction);
                if (!charge.logs.contains(neighbor) && !level.getBlockState(neighbor).is(ModTags.CHARCOAL_SEALANTS)) {
                    return neighbor;
                }
            }
        }
        return charge.opening;
    }

    private static void finish(ServerLevel level, Charge charge, float yield, BlockPos breachedAt) {
        int consumed = 0;
        for (BlockPos pos : charge.logs) {
            if (!level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS)) continue;
            level.removeBlock(pos, false);
            consumed++;
        }
        int output = Mth.floor(consumed * yield);
        if (output > 0) {
            BlockPos drop = breachedAt;
            for (int i = 0; i < 4; i++) {
                if (level.getBlockState(drop).getCollisionShape(level, drop).isEmpty()) break;
                drop = drop.above();
            }
            if (!level.getBlockState(drop).getCollisionShape(level, drop).isEmpty()) {
                drop = charge.opening.above();
            }
            while (output > 0) {
                int stackSize = Math.min(output, Items.CHARCOAL.getDefaultMaxStackSize());
                Containers.dropItemStack(level, drop.getX() + 0.5, drop.getY() + 0.5, drop.getZ() + 0.5,
                        new ItemStack(Items.CHARCOAL, stackSize));
                output -= stackSize;
            }
        }
        level.playSound(null, charge.opening, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.75F);
    }

    private static void smoke(ServerLevel level, BlockPos opening, int count) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                opening.getX() + 0.5, opening.getY() + 1.05, opening.getZ() + 0.5,
                count, 0.12, 0.04, 0.12, 0.005);
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
            try {
                phase = Phase.valueOf(entry.getString("Phase"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            BlockPos pendingBreach = entry.contains("PendingBreach") ? BlockPos.of(entry.getLong("PendingBreach")) : null;
            long pendingBreachTime = entry.getLong("PendingBreachTime");
            data.charges.add(new Charge(logs, BlockPos.of(entry.getLong("Opening")),
                    phase, entry.getLong("Deadline"), pendingBreach, pendingBreachTime));
        }
        return data;
    }

    public record IgnitionResult(boolean success, Component message) {
        static IgnitionResult success(Component message) { return new IgnitionResult(true, message); }
        static IgnitionResult failure(Component message) { return new IgnitionResult(false, message); }
    }

    private enum Phase { WAITING_FOR_SEAL, CARBONIZING, READY }

    private static final class Charge {
        private final Set<BlockPos> logs;
        private final BlockPos opening;
        private Phase phase;
        private long deadline;
        private net.minecraft.core.BlockPos pendingBreach;
        private long pendingBreachTime;

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline) {
            this(logs, opening, phase, deadline, null, 0L);
        }

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline,
                net.minecraft.core.BlockPos pendingBreach, long pendingBreachTime) {
            this.logs = Set.copyOf(logs);
            this.opening = opening.immutable();
            this.phase = phase;
            this.deadline = deadline;
            this.pendingBreach = pendingBreach != null ? pendingBreach.immutable() : null;
            this.pendingBreachTime = pendingBreachTime;
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
