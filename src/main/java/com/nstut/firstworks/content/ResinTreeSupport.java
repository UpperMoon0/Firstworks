package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Shared validation for resin-bearing trees. */
public final class ResinTreeSupport {
    private static final int MAX_CONNECTED_LOGS = 512;

    public static boolean isLivingTree(LevelReader level, BlockPos origin) {
        if (!level.getBlockState(origin).is(ModTags.RESIN_TREES)) {
            return false;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_CONNECTED_LOGS) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            BlockState currentState = level.getBlockState(current);
            if (!currentState.is(ModTags.RESIN_TREES)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(BlockTags.LEAVES)) {
                    return true;
                }
                if (neighborState.is(ModTags.RESIN_TREES) && !visited.contains(neighbor)) {
                    queue.addLast(neighbor.immutable());
                }
            }
        }

        return false;
    }

    private ResinTreeSupport() {}
}
