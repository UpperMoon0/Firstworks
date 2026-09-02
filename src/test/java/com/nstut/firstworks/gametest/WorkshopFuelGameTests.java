package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkshopFuelGameTests {
    private static final String EMPTY = "empty";

    private WorkshopFuelGameTests() {}

    @GameTest(template = EMPTY, timeoutTicks = 20)
    public static void fuelTopUpPreservesRunningProgress(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos furnacePos = new BlockPos(4, 1, 4);
        helper.setBlock(furnacePos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace = helper.getBlockEntity(furnacePos);

        check(helper, furnace.getItemHandler(null)
                .insertItem(0, new ItemStack(Items.RAW_COPPER, 3), false).isEmpty(),
                "Crucible Furnace rejected raw copper test input");
        check(helper, furnace.getItemHandler(null)
                .insertItem(1, new ItemStack(ModItems.CASTING_MOLD.get()), false).isEmpty(),
                "Crucible Furnace rejected casting mold test catalyst");
        check(helper, furnace.getItemHandler(null)
                .insertItem(2, new ItemStack(Items.CHARCOAL), false).isEmpty(),
                "Crucible Furnace rejected initial fuel");
        check(helper, furnace.stoke(160), "Crucible Furnace could not be stoked for test setup");

        tickHeated(level, helper.absolutePos(furnacePos), furnace, 20);
        int progressBeforeTopUp = furnace.getProgress();
        check(helper, progressBeforeTopUp == 20, "Crucible Furnace did not reach expected running progress");
        check(helper, furnace.isRunning(), "Crucible Furnace was not running before fuel top-up");

        ItemStack remainder = furnace.getItemHandler(null)
                .insertItem(2, new ItemStack(Items.CHARCOAL, 3), false);
        check(helper, remainder.isEmpty(), "Crucible Furnace rejected reserve fuel while running");
        check(helper, furnace.getProgress() == progressBeforeTopUp,
                "Fuel top-up reset active Crucible Furnace progress");
        check(helper, furnace.isRunning(), "Fuel top-up cleared running state");
        check(helper, furnace.getFuel().getCount() == 3,
                "Fuel top-up unexpectedly consumed reserve fuel immediately");

        tickHeated(level, helper.absolutePos(furnacePos), furnace, 1);
        check(helper, furnace.getProgress() == progressBeforeTopUp + 1,
                "Crucible Furnace failed to continue from preserved progress after fuel top-up");
        check(helper, furnace.getFuel().getCount() == 3,
                "Running Crucible Furnace consumed a second fuel item after reserve top-up");

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
