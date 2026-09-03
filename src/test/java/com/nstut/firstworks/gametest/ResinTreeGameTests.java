package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.ResinScarBlock;
import com.nstut.firstworks.content.ResinTreeSupport;
import com.nstut.firstworks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResinTreeGameTests {
    private static final String EMPTY = "empty";

    private ResinTreeGameTests() {}

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void resinTreesRequireConnectedLeafCanopy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos root = new BlockPos(4, 1, 4);
        helper.setBlock(root, Blocks.SPRUCE_LOG);

        check(helper, !ResinTreeSupport.isLivingTree(level, helper.absolutePos(root)),
                "Isolated resin-tagged log was incorrectly treated as a living tree");

        helper.setBlock(root.above(), Blocks.SPRUCE_LOG);
        helper.setBlock(root.above(2), Blocks.SPRUCE_LEAVES);
        check(helper, ResinTreeSupport.isLivingTree(level, helper.absolutePos(root)),
                "Connected resin-tagged trunk with leaves was not recognized as a living tree");

        helper.setBlock(root.above(), Blocks.AIR);
        check(helper, !ResinTreeSupport.isLivingTree(level, helper.absolutePos(root)),
                "Disconnected canopy still qualified the resin log as a living tree");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void resinTreesRejectPlayerPlacedLeavesAndDeadScars(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos root = new BlockPos(4, 1, 4);
        helper.setBlock(root, Blocks.SPRUCE_LOG);
        helper.setBlock(root.above(), Blocks.SPRUCE_LOG);
        helper.setBlock(root.above(2), Blocks.SPRUCE_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, true));

        check(helper, !ResinTreeSupport.isLivingTree(level, helper.absolutePos(root)),
                "Player-placed persistent leaves incorrectly created a renewable resin tree");

        helper.setBlock(root.above(2), Blocks.SPRUCE_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, false));
        BlockPos scarPos = root.south();
        BlockState scar = ModBlocks.RESIN_SCAR.get().defaultBlockState()
                .setValue(ResinScarBlock.FACING, Direction.NORTH)
                .setValue(ResinScarBlock.AGE, 3);
        helper.setBlock(scarPos, scar);
        check(helper, ResinScarBlock.hasLivingSupport(scar, level, helper.absolutePos(scarPos)),
                "Mature resin scar did not recognize its living tree support");

        helper.setBlock(root.above(2), Blocks.AIR);
        check(helper, !ResinScarBlock.hasLivingSupport(scar, level, helper.absolutePos(scarPos)),
                "Resin scar remained renewable after its canopy was removed");

        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
