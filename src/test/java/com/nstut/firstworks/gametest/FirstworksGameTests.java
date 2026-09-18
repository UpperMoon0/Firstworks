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
import net.minecraft.world.item.context.BlockPlaceContext;
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

    private static ItemStack smelt(GameTestHelper helper, BlockPos pos, ItemStack input, int ticks) {
        helper.setBlock(pos, Blocks.FURNACE);
        net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace = helper.getBlockEntity(pos);
        furnace.setItem(0, input);
        furnace.setItem(1, new ItemStack(Items.COAL));
        BlockPos absolute = helper.absolutePos(pos);
        for (int i = 0; i <= ticks; i++) {
            net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(
                    helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
        }
        return furnace.getItem(2).copy();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void furnaceRecipesReplaceKilnWithoutCopperBypass(GameTestHelper helper) {
        var level = helper.getLevel();
        // Exercise the same rewrite used on login and datapack reload, twice to check idempotence.
        for (int pass = 0; pass < 2; pass++) {
            com.nstut.firstworks.ToolBindingRecipes.bindVanillaTools(
                    new net.neoforged.neoforge.event.OnDatapackSyncEvent(level.getServer().getPlayerList(), null));
        }
        var manager = level.getRecipeManager();
        for (var input : List.of(Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE)) {
            var recipeInput = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(input));
            check(helper, manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    recipeInput, level).isEmpty(), "Copper still smelts directly in furnace");
            check(helper, manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING,
                    recipeInput, level).isEmpty(), "Copper still smelts directly in blast furnace");
        }
        check(helper, manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level).isPresent(),
                "Unrelated iron smelting was removed");
        var inputs = List.of(ModItems.UNFIRED_CASTING_MOLD.get(), ModItems.UNFIRED_CRUCIBLE.get(),
                ModItems.UNFIRED_TUYERE.get(), ModItems.UNFIRED_REFRACTORY_BRICK.get(),
                ModItems.CAST_COPPER_BILLET.get(), Items.CALCITE);
        var outputs = List.of(ModItems.CASTING_MOLD.get(), ModItems.CRUCIBLE.get(), ModItems.TUYERE.get(),
                ModItems.REFRACTORY_BRICK.get(), ModItems.ANNEALED_COPPER_BILLET.get(), ModItems.LIME.get());
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack result = smelt(helper, new BlockPos(2 + i, 1, 2), new ItemStack(inputs.get(i)), 250);
            check(helper, result.is(outputs.get(i)) && result.getCount() == (i == 5 ? 2 : 1),
                    "Migrated furnace recipe produced wrong result: " + inputs.get(i));
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void copperWorkshopCompletesEntirePrimitiveChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos wheelPos = new BlockPos(2, 1, 2);
        helper.setBlock(wheelPos, ModBlocks.POTTERY_WHEEL.get());
        WorkshopBlockEntity wheel = helper.getBlockEntity(wheelPos);
        hold(player, new ItemStack(ModItems.REFRACTORY_CLAY.get(), 2));
        helper.useBlock(wheelPos, player);
        helper.useBlock(wheelPos, player);
        check(helper, wheel.getInput().getCount() == 2, "Pottery Wheel did not accept two refractory clay inputs");
        clearHand(player);
        use(helper, wheelPos, player, 8);
        check(helper, wheel.getOutput().is(ModItems.UNFIRED_CASTING_MOLD.get()), "Pottery Wheel did not shape an unfired casting mold");

        ItemStack moldResult = smelt(helper, new BlockPos(5, 1, 2), wheel.getOutput().copy(), 220);
        check(helper, moldResult.is(ModItems.CASTING_MOLD.get()), "Vanilla furnace did not process mold");

        BlockPos bellowsPos = new BlockPos(7, 1, 3);
        BlockPos furnacePos = new BlockPos(8, 1, 3);
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());
        helper.setBlock(bellowsPos, ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST));
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);

        hold(player, new ItemStack(Items.RAW_COPPER, 3));
        use(helper, furnacePos, player, 3);
        hold(player, moldResult.copy());
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

        ItemStack annealResult = smelt(helper, new BlockPos(11, 1, 3), furnace.getOutput().copy(), 140);
        check(helper, annealResult.is(ModItems.ANNEALED_COPPER_BILLET.get()), "Vanilla furnace did not process anneal");

        BlockPos anvilPos = new BlockPos(13, 1, 3);
        helper.setBlock(anvilPos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil = helper.getBlockEntity(anvilPos);
        hold(player, annealResult.copy());
        helper.useBlock(anvilPos, player);
        hold(player, new ItemStack(ModItems.STONE_HAMMER.get()));
        helper.setBlock(anvilPos.south(), net.minecraft.world.level.block.Blocks.CAMPFIRE);
        check(helper, anvil.reheat(player), "Stone Anvil could not reheat beside campfire");
        var actions = anvil.activeRecipe().orElseThrow().value().forge().orElseThrow().actions();
        for (int i = 0; i < actions.size(); i++) {
            final int step = i;
            helper.runAtTickTime(i + 1, () -> {
                check(helper, anvil.forge(player, actions.get(step)), "Stone Anvil refused ordered forge action");
                if (step == actions.size() - 1) {
                    check(helper, anvil.getOutput().is(Items.COPPER_INGOT), "Stone Anvil did not finish the vanilla copper ingot");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void bellowsPlacementFacesTheAdjacentFurnace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos bellowsPos = new BlockPos(4, 1, 4);
        BlockPos furnacePos = bellowsPos.east();
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());

        BlockPos absoluteBellowsPos = helper.absolutePos(bellowsPos);
        player.setYRot(-90.0F);
        ItemStack bellowsStack = new ItemStack(ModItems.BELLOWS.get());
        hold(player, bellowsStack);
        BlockHitResult placementHit = new BlockHitResult(
                Vec3.atCenterOf(absoluteBellowsPos), Direction.UP, absoluteBellowsPos, false);
        BlockPlaceContext placementContext = new BlockPlaceContext(
                player, InteractionHand.MAIN_HAND, bellowsStack, placementHit);
        BlockState placedState = ModBlocks.BELLOWS.get().getStateForPlacement(placementContext);
        if (placedState == null) {
            helper.fail("Bellows did not produce a placement state");
            return;
        }
        check(helper, placedState.getValue(BellowsBlock.FACING) == Direction.EAST,
                "Bellows nozzle did not face in the player's placement direction toward the furnace");

        level.setBlock(absoluteBellowsPos, placedState, Block.UPDATE_ALL);
        clearHand(player);
        helper.useBlock(bellowsPos, player);
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);
        check(helper, furnace.getStokeTicks() > 0,
                "Naturally placed Bellows did not stoke the furnace in front of its nozzle");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void manualMachinesRequireAndCompleteRealPlayerWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        BlockPos quernPos = new BlockPos(3, 1, 8);
        helper.setBlock(quernPos, ModBlocks.QUERN.get());
        QuernBlockEntity quern = helper.getBlockEntity(quernPos);
        hold(player, new ItemStack(Items.WHEAT, 4));
        use(helper, quernPos, player, 4);
        check(helper, quern.getInput().getCount() == 4, "Quern did not load the wheat batch");
        check(helper,
                ModBlocks.QUERN.get().getTicker(level, helper.getBlockState(quernPos), ModBlockEntities.QUERN.get()) == null,
                "Quern unexpectedly has a server-side autonomous ticker");

        int requiredWork = quern.requiredWork();
        int workPerCrank = Math.max(1, FirstworksConfig.QUERN_MANUAL_WORK_PER_CRANK.get());
        int cranks = Math.max(1, (requiredWork + workPerCrank - 1) / workPerCrank);
        clearHand(player);
        use(helper, quernPos, player, cranks);
        check(helper, quern.getOutput().is(ModItems.FLOUR.get()) && quern.getOutput().getCount() == 4,
                "Quern did not complete the loaded wheat batch from manual cranks");

        BlockPos loomPos = new BlockPos(9, 1, 8);
        helper.setBlock(loomPos, ModBlocks.LOOM.get());
        LoomBlockEntity loom = helper.getBlockEntity(loomPos);
        hold(player, new ItemStack(Items.STRING, 4));
        use(helper, loomPos, player, 4);
        int strokes = loom.getMatchingRecipe()
                .map(holder -> Math.max(1, holder.value().strokes()))
                .orElseThrow(() -> new IllegalStateException("Loom recipe missing at runtime"));
        clearHand(player);
        for (int i = 0; i < strokes; i++) {
            final int pass = i;
            helper.runAtTickTime(i + 1, () -> {
                if (!loom.getShed().equals(pass % 2 == 0 ? "A" : "B")) loom.changeShed();
                check(helper, loom.weave(player, pass % 2 != 0), "Loom rejected basic fallback weave pass");
                if (pass == strokes - 1) {
                    check(helper, loom.getOutput().is(ModItems.CLOTH.get()), "Loom did not complete cloth from real manual passes");
                    helper.succeed();
                }
            });
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void workshopEmptyCatalystAndFuelRoutingStaySafe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos heatedPos = new BlockPos(4, 1, 4);
        helper.setBlock(heatedPos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity heated = helper.getBlockEntity(heatedPos);

        hold(player, new ItemStack(Items.CHARCOAL, 2));
        helper.useBlock(heatedPos, player);
        check(helper, heated.getInput().is(Items.CHARCOAL) && heated.getInput().getCount() == 1,
                "normal insertion did not prefer the custom recipe input role over fuel");
        check(helper, heated.getFuel().isEmpty(),
                "normal insertion routed an overlapping charcoal recipe input into fuel");
        check(helper, heated.activeRecipe().isEmpty(),
                "recipe with an explicitly declared empty catalyst tag became active");

        player.setShiftKeyDown(true);
        helper.useBlock(heatedPos, player);
        player.setShiftKeyDown(false);
        check(helper, heated.getInput().getCount() == 1,
                "sneak fuel insertion modified the loaded recipe input");
        check(helper, heated.getFuel().is(Items.CHARCOAL) && heated.getFuel().getCount() == 1,
                "sneak-right-click did not explicitly route overlapping charcoal into fuel");

        tickHeated(level, helper.absolutePos(heatedPos), heated, 4);
        check(helper, heated.getOutput().isEmpty(),
                "empty catalyst tag bypass produced output despite an unsatisfied catalyst requirement");
        check(helper, heated.getFuel().getCount() == 1,
                "Furnace consumed fuel for a recipe whose declared catalyst cannot match");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void workshopBlocksOperateAtBothBuildHeightEdges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;

        check(helper, !ModBlocks.CRUCIBLE_FURNACE.get().defaultBlockState().canOcclude(),
                "Crucible furnace must not occlude neighboring faces around its hollow model");
        check(helper, !ModBlocks.CRUCIBLE_FURNACE.get().defaultBlockState().canOcclude(),
                "Crucible Furnace must not occlude neighboring faces around its hollow model");
        check(helper, !level.isOutsideBuildHeight(minY), "Vanilla lower build edge was treated as out of bounds");
        check(helper, !level.isOutsideBuildHeight(maxY), "Vanilla upper build edge was treated as out of bounds");
        check(helper, level.isOutsideBuildHeight(minY - 1), "Layer below vanilla minimum was unexpectedly valid");
        check(helper, level.isOutsideBuildHeight(maxY + 1), "Layer above vanilla maximum was unexpectedly valid");

        List<Block> blocks = List.of(
                ModBlocks.POTTERY_WHEEL.get(),
                ModBlocks.STONE_ANVIL.get(),
                ModBlocks.BELLOWS.get(),
                ModBlocks.CRUCIBLE_FURNACE.get(),
                ModBlocks.LOOM.get(),
                ModBlocks.QUERN.get());
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
            for (int i = 0; i < 8; i++) {
                check(helper, wheel.work(player), "Pottery Wheel refused manual work at Y=" + y + " step=" + i);
            }
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
