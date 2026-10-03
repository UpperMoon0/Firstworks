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
    public static void crucibleMoldSlotIsSingleAndLegacySurplusIsReturned(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace = helper.getBlockEntity(pos);
        var handler = furnace.getItemHandler(null);
        check(helper, handler.getSlotLimit(1) == 1 && handler.getSlotLimit(2) == 64, "Wrong mold/fuel limits");
        var molds = new ItemStack(ModItems.CASTING_MOLD.get(), 3);
        check(helper, handler.insertItem(1, molds, true).getCount() == 2 && furnace.getCatalyst().isEmpty(),
                "Simulation did not preserve the single-mold limit");
        check(helper, handler.insertItem(1, molds, false).getCount() == 2 && furnace.getCatalyst().getCount() == 1,
                "Automation inserted more than one mold");
        check(helper, !furnace.insert(molds, false) && molds.getCount() == 3, "Player inserted a second mold");
        check(helper, handler.insertItem(0, new ItemStack(Items.RAW_COPPER, 8), false).isEmpty()
                        && handler.insertItem(2, new ItemStack(Items.COAL, 15), false).isEmpty(),
                "Mold limit also restricted input or fuel");
        var saved = furnace.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.put("Catalyst", molds.save(helper.getLevel().registryAccess()));
        furnace.loadWithComponents(saved, helper.getLevel().registryAccess());
        for (int i = 0; i < 2; i++) WorkshopBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), furnace.getBlockState(), furnace);
        check(helper, furnace.getCatalyst().getCount() == 1, "Legacy stack was not reduced to one mold");
        int returned = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2)).stream()
                .filter(entity -> entity.getItem().is(ModItems.CASTING_MOLD.get())).mapToInt(entity -> entity.getItem().getCount()).sum();
        check(helper, returned == 2, "Legacy surplus was lost or duplicated: " + returned);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        check(helper, furnace.takeStored(player), "Mold could not be retrieved");
        var held = new ItemStack(ModItems.CASTING_MOLD.get(), 3);
        check(helper, furnace.insert(held, false) && held.getCount() == 2 && furnace.getCatalyst().getCount() == 1,
                "Player insertion did not consume exactly one mold");
        helper.succeed();
    }

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
        check(helper, furnace.ignite(), "Explicit ignition failed");

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
        ThermalTestSupport.tickHot(level, pos, workshop, ticks);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
