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
    public static final int MIN_LOGS = 4;
    public static final int MAX_LOGS = 64;
    public static final int SEAL_WINDOW_TICKS = 1_200;
    public static final int CARBONIZATION_TICKS = 6_000;
    private static final String DATA_NAME = "firstworks_charcoal_mounds";
    private static final float NORMAL_YIELD = 0.75F;
    private static final float BREACHED_YIELD = 0.25F;

    private final List<Charge> charges = new ArrayList<>();

    public static CharcoalMoundData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CharcoalMoundData::new, CharcoalMoundData::load), DATA_NAME);
    }

    public IgnitionResult ignite(ServerLevel level, BlockPos ignitionLog, Direction exposedFace) {
        Set<BlockPos> logs = discoverLogs(level, ignitionLog);
        if (logs.size() < MIN_LOGS) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.too_small", MIN_LOGS));
        }
        if (logs.size() > MAX_LOGS) {
            return IgnitionResult.failure(Component.translatable("message.firstworks.charcoal.too_large", MAX_LOGS));
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
                level.getGameTime() + SEAL_WINDOW_TICKS));
        setDirty();
        level.playSound(null, ignitionLog, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.7F);
        return IgnitionResult.success(Component.translatable("message.firstworks.charcoal.seal_opening"));
    }

    public void tick(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L || charges.isEmpty()) return;
        Iterator<Charge> iterator = charges.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            Charge charge = iterator.next();
            if (!charge.isLoaded(level)) continue;

            if (charge.phase == Phase.WAITING_FOR_SEAL) {
                if (!charge.logsIntact(level)
                        || !isShellValid(level, charge.logs, charge.opening, true)
                        || level.getGameTime() >= charge.deadline) {
                    iterator.remove();
                    changed = true;
                    continue;
                }
                smoke(level, charge.opening, 2);
                if (level.getBlockState(charge.opening).is(ModTags.CHARCOAL_SEALANTS)) {
                    charge.phase = Phase.CARBONIZING;
                    charge.deadline = level.getGameTime() + CARBONIZATION_TICKS;
                    level.playSound(null, charge.opening, SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS, 0.55F, 0.65F);
                    changed = true;
                }
                continue;
            }

            if (!charge.logsIntact(level) || !isShellValid(level, charge.logs, charge.opening, false)) {
                finish(level, charge, BREACHED_YIELD);
                iterator.remove();
                changed = true;
                continue;
            }
            smoke(level, charge.opening, 1);
            if (level.getGameTime() >= charge.deadline) {
                finish(level, charge, NORMAL_YIELD);
                iterator.remove();
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    private static Set<BlockPos> discoverLogs(ServerLevel level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        while (!queue.isEmpty() && found.size() <= MAX_LOGS) {
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
                if (!level.getBlockState(neighbor).is(ModTags.CHARCOAL_SEALANTS)) return false;
            }
        }
        return true;
    }

    private static void finish(ServerLevel level, Charge charge, float yield) {
        int consumed = 0;
        for (BlockPos pos : charge.logs) {
            if (!level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS)) continue;
            level.removeBlock(pos, false);
            consumed++;
        }
        int output = Mth.floor(consumed * yield);
        if (output > 0) {
            BlockPos drop = charge.opening;
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
            if (logs.isEmpty() || logs.size() > MAX_LOGS) continue;
            Phase phase;
            try {
                phase = Phase.valueOf(entry.getString("Phase"));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            data.charges.add(new Charge(logs, BlockPos.of(entry.getLong("Opening")),
                    phase, entry.getLong("Deadline")));
        }
        return data;
    }

    public record IgnitionResult(boolean success, Component message) {
        static IgnitionResult success(Component message) { return new IgnitionResult(true, message); }
        static IgnitionResult failure(Component message) { return new IgnitionResult(false, message); }
    }

    private enum Phase { WAITING_FOR_SEAL, CARBONIZING }

    private static final class Charge {
        private final Set<BlockPos> logs;
        private final BlockPos opening;
        private Phase phase;
        private long deadline;

        private Charge(Set<BlockPos> logs, BlockPos opening, Phase phase, long deadline) {
            this.logs = Set.copyOf(logs);
            this.opening = opening.immutable();
            this.phase = phase;
            this.deadline = deadline;
        }

        private boolean overlaps(Set<BlockPos> other) {
            return other.stream().anyMatch(logs::contains);
        }

        private boolean isLoaded(ServerLevel level) {
            return level.hasChunkAt(opening) && logs.stream().allMatch(level::hasChunkAt);
        }

        private boolean logsIntact(ServerLevel level) {
            return logs.stream().allMatch(pos -> level.getBlockState(pos).is(ModTags.CHARCOAL_WOODS));
        }
    }
}
