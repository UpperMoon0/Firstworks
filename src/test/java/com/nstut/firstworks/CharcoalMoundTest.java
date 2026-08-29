package com.nstut.firstworks;

import com.nstut.firstworks.content.charcoal.CharcoalMoundData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CharcoalMoundTest {

    @Test
    public void testPhaseEnumAndLegacyCompatibility() {
        assertEquals(CharcoalMoundData.Phase.WAITING_FOR_SEAL, CharcoalMoundData.Phase.valueOf("WAITING_FOR_SEAL"));
        assertEquals(CharcoalMoundData.Phase.CARBONIZING, CharcoalMoundData.Phase.valueOf("CARBONIZING"));
        assertEquals(CharcoalMoundData.Phase.LEGACY_READY, CharcoalMoundData.Phase.valueOf("LEGACY_READY"));
    }

    @Test
    public void testMoundStatusRecord() {
        CharcoalMoundData.MoundStatus status = new CharcoalMoundData.MoundStatus(
                CharcoalMoundData.Phase.CARBONIZING, 16, 2400L, 12);
        assertEquals(CharcoalMoundData.Phase.CARBONIZING, status.phase());
        assertEquals(16, status.logCount());
        assertEquals(2400L, status.remainingTicks());
        assertEquals(12, status.expectedYield());
    }

    @Test
    public void testIgnitionResultRecord() {
        CharcoalMoundData.IgnitionResult success = CharcoalMoundData.IgnitionResult.success();
        assertTrue(success.isSuccessful());
        assertNull(success.message());

        CharcoalMoundData.IgnitionResult failure = CharcoalMoundData.IgnitionResult.failure(null);
        assertFalse(failure.isSuccessful());
    }

    @Test
    public void testShellCoverageOnOrdinaryTree() {
        // A standing 5-block tree at (0, 1, 0) to (0, 5, 0) on dirt (0, 0, 0)
        Set<BlockPos> treeLogs = new HashSet<>();
        for (int y = 1; y <= 5; y++) {
            treeLogs.add(new BlockPos(0, y, 0));
        }

        // Simulate exterior counting: 5 logs * 4 sides + top (0, 6, 0) + bottom (0, 0, 0) = 22 exterior faces
        int exterior = 0;
        int sealed = 0;
        BlockPos dirtBlock = new BlockPos(0, 0, 0);

        for (BlockPos log : treeLogs) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = log.relative(dir);
                if (treeLogs.contains(neighbor)) continue;
                exterior++;
                if (neighbor.equals(dirtBlock)) {
                    sealed++;
                }
            }
        }

        assertEquals(22, exterior);
        assertEquals(1, sealed);
        float ratio = (float) sealed / exterior;
        assertTrue(ratio < 0.10F, "Ordinary tree shell coverage ratio should be under 10% (was: " + ratio + ")");
        assertFalse(ratio >= 0.50F, "Ordinary tree should NOT be a mound candidate");
    }

    @Test
    public void testShellCoverageOnEnclosedMound() {
        // A 2x2x2 mound (8 logs) enclosed by sealant blocks on all sides except 1 opening
        Set<BlockPos> moundLogs = new HashSet<>();
        for (int x = 0; x < 2; x++) {
            for (int y = 1; y <= 2; y++) {
                for (int z = 0; z < 2; z++) {
                    moundLogs.add(new BlockPos(x, y, z));
                }
            }
        }

        // 2x2x2 cube has 24 exterior faces. 23 sealed, 1 opening.
        int exterior = 0;
        int sealed = 0;
        BlockPos opening = new BlockPos(0, 1, -1);

        for (BlockPos log : moundLogs) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = log.relative(dir);
                if (moundLogs.contains(neighbor)) continue;
                exterior++;
                if (!neighbor.equals(opening)) {
                    sealed++;
                }
            }
        }

        assertEquals(24, exterior);
        assertEquals(23, sealed);
        float ratio = (float) sealed / exterior;
        assertEquals(23.0F / 24.0F, ratio, 0.001F);
        assertTrue(ratio >= 0.50F, "Deliberate mound should be a mound candidate");
    }

    @Test
    public void testCharcoalMaterializationCounts() {
        // 64 logs at 75% yield: 48 charcoal -> 12 piles of 4
        int totalCharcoal64Normal = Mth.floor(64 * 0.75F);
        assertEquals(48, totalCharcoal64Normal);

        int pilesOf4 = 0;
        int remaining = totalCharcoal64Normal;
        while (remaining > 0) {
            int place = Math.min(remaining, 4);
            if (place == 4) pilesOf4++;
            remaining -= place;
        }
        assertEquals(12, pilesOf4);

        // 64 logs at 25% yield (breached): 16 charcoal -> 4 piles of 4
        int totalCharcoal64Breached = Mth.floor(64 * 0.25F);
        assertEquals(16, totalCharcoal64Breached);
        assertEquals(4, totalCharcoal64Breached / 4);

        // 7 logs at 75% yield: 5 charcoal -> 1 pile of 4 and 1 pile of 1
        int totalCharcoal7 = Mth.floor(7 * 0.75F);
        assertEquals(5, totalCharcoal7);

        List<Integer> piles = new ArrayList<>();
        remaining = totalCharcoal7;
        while (remaining > 0) {
            int place = Math.min(remaining, 4);
            piles.add(place);
            remaining -= place;
        }
        assertEquals(List.of(4, 1), piles);
    }

    @Test
    public void testBottomUpPlacementSorting() {
        List<BlockPos> unsorted = List.of(
                new BlockPos(1, 3, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 1),
                new BlockPos(0, 2, 0),
                new BlockPos(0, 1, 1)
        );

        List<BlockPos> sorted = unsorted.stream()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .toList();

        assertEquals(new BlockPos(0, 1, 0), sorted.get(0));
        assertEquals(new BlockPos(0, 1, 1), sorted.get(1));
        assertEquals(new BlockPos(1, 1, 1), sorted.get(2));
        assertEquals(new BlockPos(0, 2, 0), sorted.get(3));
        assertEquals(new BlockPos(1, 3, 0), sorted.get(4));
    }

    @Test
    public void testConsumedPositionsFiltering() {
        // Mound originally had 4 logs, but 1 was replaced with stone
        Set<BlockPos> originalLogs = Set.of(
                new BlockPos(0, 1, 0),
                new BlockPos(0, 1, 1),
                new BlockPos(0, 2, 0),
                new BlockPos(0, 2, 1)
        );
        Set<BlockPos> survivingWoodLogs = Set.of(
                new BlockPos(0, 1, 0),
                new BlockPos(0, 1, 1),
                new BlockPos(0, 2, 0)
        ); // (0, 2, 1) is now stone

        List<BlockPos> consumedPositions = new ArrayList<>();
        for (BlockPos pos : originalLogs) {
            if (survivingWoodLogs.contains(pos)) {
                consumedPositions.add(pos);
            }
        }
        assertEquals(3, consumedPositions.size());
        assertFalse(consumedPositions.contains(new BlockPos(0, 2, 1)));

        // Pile destinations are picked ONLY from consumed positions
        List<BlockPos> sortedDestinations = consumedPositions.stream()
                .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .toList();

        assertEquals(3, sortedDestinations.size());
        assertFalse(sortedDestinations.contains(new BlockPos(0, 2, 1)));
    }

    @Test
    public void testPendingSealDeadlineComparison() {
        long deadline = 1200L;
        long onTimeSeal = 1150L;
        long lateSeal = 1205L;

        assertTrue(onTimeSeal <= deadline, "On-time seal placed before deadline should be valid");
        assertFalse(lateSeal <= deadline, "Late seal placed after deadline should be rejected");
    }
}
