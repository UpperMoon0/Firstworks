package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.quern.QuernBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import com.nstut.firstworks.content.workshop.WorkshopRecipeInput;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkshopSelectionGameTests {
    private static final String EMPTY = "empty";

    private WorkshopSelectionGameTests() {}

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void catalystSpecificRecipeWinsEqualBatchTie(GameTestHelper helper) {
        BlockPos wheelPos = new BlockPos(4, 1, 4);
        helper.setBlock(wheelPos, ModBlocks.POTTERY_WHEEL.get());
        WorkshopBlockEntity wheel = helper.getBlockEntity(wheelPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        check(helper, wheel.getItemHandler(null)
                .insertItem(0, new ItemStack(Items.AMETHYST_SHARD), false).isEmpty(),
                "Pottery Wheel rejected catalyst-selection test input");
        check(helper, wheel.getItemHandler(null)
                .insertItem(1, new ItemStack(Items.STICK), false).isEmpty(),
                "Pottery Wheel rejected catalyst-selection test catalyst");

        var selected = wheel.activeRecipe().orElseThrow(
                () -> new IllegalStateException("No workshop recipe selected for catalyst precedence test"));
        check(helper, selected.id().getPath().equals("gametest_z_catalyst_specific"),
                "Catalyst-free fallback shadowed an equally sized catalyst-specific recipe");
        check(helper, selected.value().result().is(Items.DIAMOND),
                "Catalyst-specific recipe did not provide the expected result");

        check(helper, wheel.work(player), "Pottery Wheel refused catalyst-specific recipe work");
        check(helper, wheel.getOutput().is(Items.DIAMOND),
                "Pottery Wheel completed the wrong equal-batch workshop recipe");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void standardRecipeManagerHonorsWorkshopStationAndCatalyst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var recipes = level.getRecipeManager();

        var wrongStation = recipes.getRecipeFor(
                ModRecipes.WORKSHOP_PROCESSING_TYPE.get(),
                new WorkshopRecipeInput(WorkshopRecipe.KILN,
                        new ItemStack(Items.EMERALD), new ItemStack(Items.STICK)),
                level);
        check(helper, wrongStation.isEmpty(),
                "RecipeManager matched a Pottery Wheel workshop recipe at the Kiln station");

        var missingCatalyst = recipes.getRecipeFor(
                ModRecipes.WORKSHOP_PROCESSING_TYPE.get(),
                new WorkshopRecipeInput(WorkshopRecipe.POTTERY_WHEEL,
                        new ItemStack(Items.EMERALD), ItemStack.EMPTY),
                level);
        check(helper, missingCatalyst.isEmpty(),
                "RecipeManager matched a catalyst-required workshop recipe without its catalyst");

        var matched = recipes.getRecipeFor(
                ModRecipes.WORKSHOP_PROCESSING_TYPE.get(),
                new WorkshopRecipeInput(WorkshopRecipe.POTTERY_WHEEL,
                        new ItemStack(Items.EMERALD), new ItemStack(Items.STICK)),
                level);
        check(helper, matched.isPresent() && matched.get().id().getPath().equals("gametest_required_catalyst"),
                "RecipeManager did not match the complete workshop station/input/catalyst state");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void quernPriorityAndRecipeIdTieBreakAreBehavioral(GameTestHelper helper) {
        BlockPos quernPos = new BlockPos(2, 1, 8);
        helper.setBlock(quernPos, ModBlocks.QUERN.get());
        QuernBlockEntity quern = helper.getBlockEntity(quernPos);

        var selected = quern.findRecipeForIngredient(new ItemStack(Items.QUARTZ)).orElseThrow(
                () -> new IllegalStateException("No overlapping quern recipe loaded for priority test"));
        check(helper, selected.value().priority() == 10,
                "Quern did not prefer the highest-priority overlapping recipe");
        check(helper, selected.id().getPath().equals("gametest_quern_priority_tie_z"),
                "Quern priority ties did not resolve deterministically by recipe id");
        check(helper, selected.value().result().is(Items.EMERALD),
                "Quern selected the wrong result after priority/id ordering");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void bellowsPressesBankFiniteAirReserve(GameTestHelper helper) {
        BlockPos bellowsPos = new BlockPos(5, 1, 4);
        BlockPos furnacePos = bellowsPos.east();
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());
        helper.setBlock(bellowsPos,
                ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING, Direction.EAST));
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.useBlock(bellowsPos, player);
        int firstPress = furnace.getStokeTicks();
        check(helper, firstPress == 160,
                "First Bellows press did not add one full airflow pulse");

        helper.useBlock(bellowsPos, player);
        check(helper, furnace.getStokeTicks() == 320,
                "Second Bellows press replaced rather than banked the existing airflow pulse");

        for (int i = 0; i < 6; i++) {
            helper.useBlock(bellowsPos, player);
        }
        check(helper, furnace.getStokeTicks() == 480,
                "Bellows airflow reserve did not clamp at the intended three-stroke capacity");

        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void stableHeatedAutomationPreservesProgressAndFuel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos furnacePos = new BlockPos(8, 1, 4);
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);

        check(helper, furnace.getItemHandler(null)
                .insertItem(0, new ItemStack(Items.RAW_COPPER, 3), false).isEmpty(),
                "Crucible Furnace rejected initial raw copper batch");
        check(helper, furnace.getItemHandler(null)
                .insertItem(1, new ItemStack(ModItems.CASTING_MOLD.get()), false).isEmpty(),
                "Crucible Furnace rejected initial casting mold");
        check(helper, furnace.getItemHandler(null)
                .insertItem(2, new ItemStack(Items.CHARCOAL, 2), false).isEmpty(),
                "Crucible Furnace rejected initial fuel reserve");
        check(helper, furnace.stoke(160), "Crucible Furnace could not be stoked for automation test");

        tickHeated(level, helper.absolutePos(furnacePos), furnace, 20);
        int progressBeforeTopUps = furnace.getProgress();
        int fuelBeforeTopUps = furnace.getFuel().getCount();
        check(helper, progressBeforeTopUps == 20,
                "Crucible Furnace did not reach expected running progress before automation top-ups");
        check(helper, furnace.isRunning(), "Crucible Furnace was not running before automation top-ups");
        check(helper, fuelBeforeTopUps == 1,
                "Crucible Furnace did not consume exactly one fuel item when starting");

        check(helper, furnace.getItemHandler(null)
                .insertItem(0, new ItemStack(Items.RAW_COPPER), false).isEmpty(),
                "Crucible Furnace rejected same-recipe input top-up while running");
        check(helper, furnace.getProgress() == progressBeforeTopUps,
                "Same-recipe input top-up reset active Crucible Furnace progress");
        check(helper, furnace.isRunning(),
                "Same-recipe input top-up cleared Crucible Furnace running state");
        check(helper, furnace.getFuel().getCount() == fuelBeforeTopUps,
                "Same-recipe input top-up consumed extra fuel");

        check(helper, furnace.getItemHandler(null)
                .insertItem(1, new ItemStack(ModItems.CASTING_MOLD.get()), false).isEmpty(),
                "Crucible Furnace rejected same-recipe catalyst top-up while running");
        check(helper, furnace.getProgress() == progressBeforeTopUps,
                "Same-recipe catalyst top-up reset active Crucible Furnace progress");
        check(helper, furnace.isRunning(),
                "Same-recipe catalyst top-up cleared Crucible Furnace running state");
        check(helper, furnace.getFuel().getCount() == fuelBeforeTopUps,
                "Same-recipe catalyst top-up consumed extra fuel");

        tickHeated(level, helper.absolutePos(furnacePos), furnace, 1);
        check(helper, furnace.getProgress() == progressBeforeTopUps + 1,
                "Crucible Furnace failed to continue from preserved progress after automation top-ups");
        check(helper, furnace.getFuel().getCount() == fuelBeforeTopUps,
                "Crucible Furnace consumed another fuel item after stable automation top-ups");

        helper.succeed();
    }

    private static void tickHeated(ServerLevel level, BlockPos pos, WorkshopBlockEntity workshop, int ticks) {
        for (int i = 0; i < ticks; i++) {
            WorkshopBlockEntity.serverTick(level, pos, level.getBlockState(pos), workshop);
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
