package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.quern.QuernBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.registry.ModBlockEntities;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FirstworksGameTests {
    private static final String EMPTY = "empty";

    private FirstworksGameTests() {}

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void copperWorkshopCompletesEntirePrimitiveChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.CREATIVE);

        BlockPos wheelPos = new BlockPos(2, 1, 2);
        helper.setBlock(wheelPos, ModBlocks.POTTERY_WHEEL.get());
        WorkshopBlockEntity wheel = helper.getBlockEntity(wheelPos);
        hold(player, new ItemStack(ModItems.REFRACTORY_CLAY.get()));
        helper.useBlock(wheelPos, player);
        helper.useBlock(wheelPos, player);
        check(helper, wheel.getInput().getCount() == 2, "Pottery Wheel did not accept two refractory clay inputs");
        clearHand(player);
        use(helper, wheelPos, player, 8);
        check(helper, wheel.getOutput().is(ModItems.UNFIRED_CASTING_MOLD.get()), "Pottery Wheel did not shape an unfired casting mold");

        BlockPos moldKilnPos = new BlockPos(5, 1, 2);
        helper.setBlock(moldKilnPos, ModBlocks.KILN.get());
        WorkshopBlockEntity moldKiln = helper.getBlockEntity(moldKilnPos);
        hold(player, wheel.getOutput().copy());
        helper.useBlock(moldKilnPos, player);
        hold(player, new ItemStack(Items.COAL));
        helper.useBlock(moldKilnPos, player);
        clearHand(player);
        tickHeated(level, helper.absolutePos(moldKilnPos), moldKiln, 220);
        check(helper, moldKiln.getOutput().is(ModItems.CASTING_MOLD.get()), "Kiln did not fire the casting mold");

        BlockPos bellowsPos = new BlockPos(7, 1, 3);
        BlockPos furnacePos = new BlockPos(8, 1, 3);
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());
        helper.setBlock(bellowsPos, ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST));
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);

        hold(player, new ItemStack(Items.RAW_COPPER));
        use(helper, furnacePos, player, 3);
        hold(player, moldKiln.getOutput().copy());
        helper.useBlock(furnacePos, player);
        hold(player, new ItemStack(Items.CHARCOAL));
        helper.useBlock(furnacePos, player);
        clearHand(player);

        tickHeated(level, helper.absolutePos(furnacePos), furnace, 1);
        check(helper, furnace.getProgress() == 0, "Crucible Furnace progressed without Bellows air");
        check(helper, !furnace.getFuel().isEmpty(), "Crucible Furnace consumed fuel while starved of air");

        helper.useBlock(bellowsPos, player);
        check(helper, furnace.getStokeTicks() > 0, "Bellows did not stoke the adjacent Crucible Furnace");
        tickHeated(level, helper.absolutePos(furnacePos), furnace, 120);
        helper.useBlock(bellowsPos, player);
        tickHeated(level, helper.absolutePos(furnacePos), furnace, 120);
        check(helper, furnace.getOutput().is(ModItems.CAST_COPPER_BILLET.get()), "Crucible Furnace did not cast a copper billet");
        check(helper, furnace.getCatalyst().is(ModItems.CASTING_MOLD.get()), "Reusable casting mold was consumed");

        BlockPos annealKilnPos = new BlockPos(11, 1, 3);
        helper.setBlock(annealKilnPos, ModBlocks.KILN.get());
        WorkshopBlockEntity annealKiln = helper.getBlockEntity(annealKilnPos);
        hold(player, furnace.getOutput().copy());
        helper.useBlock(annealKilnPos, player);
        hold(player, new ItemStack(Items.COAL));
        helper.useBlock(annealKilnPos, player);
        clearHand(player);
        tickHeated(level, helper.absolutePos(annealKilnPos), annealKiln, 140);
        check(helper, annealKiln.getOutput().is(ModItems.ANNEALED_COPPER_BILLET.get()), "Kiln did not anneal the cast copper billet");

        BlockPos anvilPos = new BlockPos(13, 1, 3);
        helper.setBlock(anvilPos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil = helper.getBlockEntity(anvilPos);
        hold(player, annealKiln.getOutput().copy());
        helper.useBlock(anvilPos, player);
        hold(player, new ItemStack(ModItems.STONE_HAMMER.get()));
        use(helper, anvilPos, player, 8);
        check(helper, anvil.getOutput().is(ModItems.WORKED_COPPER_BILLET.get()), "Stone Anvil did not finish the worked copper billet");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void manualMachinesRequireAndCompleteRealPlayerWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.CREATIVE);

        BlockPos quernPos = new BlockPos(3, 1, 8);
        helper.setBlock(quernPos, ModBlocks.ROTARY_QUERN.get());
        QuernBlockEntity quern = helper.getBlockEntity(quernPos);
        hold(player, new ItemStack(Items.WHEAT));
        use(helper, quernPos, player, 4);
        check(helper, quern.getInput().getCount() == 4, "Rotary Quern did not load the wheat batch");
        check(helper,
                ModBlocks.ROTARY_QUERN.get().getTicker(level, helper.getBlockState(quernPos), ModBlockEntities.QUERN.get()) == null,
                "Rotary Quern unexpectedly has a server-side autonomous ticker");

        int requiredWork = quern.requiredWork();
        int workPerCrank = Math.max(1, FirstworksConfig.QUERN_MANUAL_WORK_PER_CRANK.get() * 4);
        int cranks = Math.max(1, (requiredWork + workPerCrank - 1) / workPerCrank);
        clearHand(player);
        use(helper, quernPos, player, cranks);
        check(helper, quern.getOutput().is(ModItems.FLOUR.get()) && quern.getOutput().getCount() == 4,
                "Rotary Quern did not complete the loaded wheat batch from manual cranks");

        BlockPos loomPos = new BlockPos(9, 1, 8);
        helper.setBlock(loomPos, ModBlocks.COPPER_LOOM.get());
        LoomBlockEntity loom = helper.getBlockEntity(loomPos);
        hold(player, new ItemStack(Items.STRING));
        use(helper, loomPos, player, 4);
        int strokes = loom.getMatchingRecipe()
                .map(holder -> Math.max(1, (holder.value().strokes() + 1) / 2))
                .orElseThrow(() -> new IllegalStateException("Copper Loom recipe missing at runtime"));
        clearHand(player);
        use(helper, loomPos, player, strokes);
        check(helper, loom.getOutput().is(ModItems.CLOTH.get()), "Copper Loom did not complete cloth from real manual strokes");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void workshopBlocksOperateAtBothBuildHeightEdges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        check(helper, !level.isOutsideBuildHeight(minY), "Vanilla lower build edge was treated as out of bounds");
        check(helper, !level.isOutsideBuildHeight(maxY), "Vanilla upper build edge was treated as out of bounds");
        check(helper, level.isOutsideBuildHeight(minY - 1), "Layer below vanilla minimum was unexpectedly valid");
        check(helper, level.isOutsideBuildHeight(maxY + 1), "Layer above vanilla maximum was unexpectedly valid");

        List<Block> blocks = List.of(
                ModBlocks.POTTERY_WHEEL.get(),
                ModBlocks.KILN.get(),
                ModBlocks.STONE_ANVIL.get(),
                ModBlocks.BELLOWS.get(),
                ModBlocks.CRUCIBLE_FURNACE.get(),
                ModBlocks.COPPER_LOOM.get(),
                ModBlocks.ROTARY_QUERN.get());
        BlockPos origin = helper.absolutePos(new BlockPos(2, 0, 12));

        for (int y : new int[]{minY, maxY}) {
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                BlockPos pos = new BlockPos(origin.getX() + i, y, origin.getZ());
                check(helper, level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL),
                        "Could not place " + block.getName().getString() + " at Y=" + y);
                check(helper, level.getBlockState(pos).is(block),
                        block.getName().getString() + " did not remain placed at Y=" + y);
                check(helper, level.getBlockEntity(pos) != null,
                        block.getName().getString() + " did not create its block entity at Y=" + y);
            }

            BlockPos wheelPos = new BlockPos(origin.getX(), y, origin.getZ());
            WorkshopBlockEntity wheel = (WorkshopBlockEntity) level.getBlockEntity(wheelPos);
            ItemStack remainder = wheel.getItemHandler(null).insertItem(0, new ItemStack(ModItems.REFRACTORY_CLAY.get(), 2), false);
            check(helper, remainder.isEmpty(), "Pottery Wheel could not load its recipe at Y=" + y);
            clearHand(player);
            BlockHitResult wheelHit = new BlockHitResult(Vec3.atCenterOf(wheelPos), Direction.UP, wheelPos, false);
            for (int i = 0; i < 8; i++) level.getBlockState(wheelPos).useWithoutItem(level, player, wheelHit);
            check(helper, wheel.getOutput().is(ModItems.UNFIRED_CASTING_MOLD.get()),
                    "Pottery Wheel processing failed at Y=" + y);

            BlockPos edgeBellowsPos = new BlockPos(origin.getX() + 9, y, origin.getZ() + 2);
            BlockPos edgeFurnacePos = edgeBellowsPos.east();
            level.setBlock(edgeFurnacePos, ModBlocks.CRUCIBLE_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(edgeBellowsPos,
                    ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
            WorkshopBlockEntity edgeFurnace = (WorkshopBlockEntity) level.getBlockEntity(edgeFurnacePos);
            BlockHitResult bellowsHit = new BlockHitResult(Vec3.atCenterOf(edgeBellowsPos), Direction.UP, edgeBellowsPos, false);
            level.getBlockState(edgeBellowsPos).useWithoutItem(level, player, bellowsHit);
            check(helper, edgeFurnace != null && edgeFurnace.getStokeTicks() > 0,
                    "Bellows adjacency/interaction failed at Y=" + y);

            for (int i = 0; i < blocks.size(); i++) {
                level.setBlock(new BlockPos(origin.getX() + i, y, origin.getZ()), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(edgeBellowsPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(edgeFurnacePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        helper.succeed();
    }

    private static void tickHeated(ServerLevel level, BlockPos pos, WorkshopBlockEntity workshop, int ticks) {
        for (int i = 0; i < ticks; i++) {
            WorkshopBlockEntity.serverTick(level, pos, level.getBlockState(pos), workshop);
        }
    }

    private static void use(GameTestHelper helper, BlockPos pos, Player player, int times) {
        for (int i = 0; i < times; i++) helper.useBlock(pos, player);
    }

    private static void hold(Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static void clearHand(Player player) {
        hold(player, ItemStack.EMPTY);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
