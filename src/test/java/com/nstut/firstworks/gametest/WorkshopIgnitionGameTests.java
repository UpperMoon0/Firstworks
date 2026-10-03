package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.*;
import com.nstut.firstworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkshopIgnitionGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void playerIgnitionCostsAndBothHands(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.KILN.get());
        WorkshopBlockEntity kiln = h.getBlockEntity(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(kiln.getBlockPos()), Direction.NORTH, kiln.getBlockPos(), false);
        var flint = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.OFF_HAND, flint);
        h.assertTrue(kiln.getBlockState().useWithoutItem(h.getLevel(), player, hit) == InteractionResult.PASS,
                "Empty main hand swallowed offhand ignition");
        kiln.getBlockState().useItemOn(flint, h.getLevel(), player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(flint.getDamageValue() == 0 && kiln.getBurnTicks() == 0, "Failed ignition cost durability");
        kiln.insert(new ItemStack(Items.COPPER_INGOT), false);
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        kiln.loadWithComponents(kiln.saveWithoutMetadata(h.getLevel().registryAccess()), h.getLevel().registryAccess());
        tick(h, kiln, 5);
        h.assertTrue(kiln.getBurnTicks() == 0 && kiln.getForgeHeat() == 0 && kiln.getFuel().getCount() == 1,
                "Fuel insertion magically lit kiln");
        kiln.getBlockState().useItemOn(flint, h.getLevel(), player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(flint.getDamageValue() == 1 && kiln.getBurnTicks() > 0, "Offhand flint did not ignite with one durability");
        kiln.getBlockState().useItemOn(flint, h.getLevel(), player, InteractionHand.OFF_HAND, hit);
        h.assertTrue(flint.getDamageValue() == 1, "Redundant ignition consumed durability");
        int burn = kiln.getBurnTicks();
        var saved = kiln.saveWithoutMetadata(h.getLevel().registryAccess());
        kiln.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(kiln.getBurnTicks() == burn, "Reload lost burning state");
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        tick(h, kiln, burn);
        h.assertTrue(kiln.getBurnTicks() > 0 && kiln.getFuel().isEmpty(), "Live fire did not catch queued fuel");
        tick(h, kiln, kiln.getBurnTicks());
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        tick(h, kiln, 2);
        h.assertTrue(kiln.getBurnTicks() == 0 && kiln.getFuel().getCount() == 1, "Cold kiln relit on refill");
        var starter = new ItemStack(ModItems.FIRE_STARTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, starter);
        kiln.getBlockState().useItemOn(starter, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(starter.isEmpty() && kiln.getBurnTicks() > 0, "One-use fire starter did not ignite");
        tick(h, kiln, kiln.getBurnTicks());
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        var tagged = new ItemStack(Items.BLAZE_ROD, 2);
        h.assertTrue(WorkshopIgnition.isIgniter(tagged), "Datapack igniter fallback was ignored");
        player.setItemInHand(InteractionHand.MAIN_HAND, tagged);
        kiln.getBlockState().useItemOn(tagged, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(tagged.getCount() == 1 && kiln.getBurnTicks() > 0, "Tagged ignition did not consume one item");
        tick(h, kiln, kiln.getBurnTicks());
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        var charge = new ItemStack(Items.FIRE_CHARGE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, charge);
        kiln.getBlockState().useItemOn(charge, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(charge.getCount() == 1 && kiln.getBurnTicks() > 0, "Fire charge ignition failed");
        tick(h, kiln, kiln.getBurnTicks());
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        player.getAbilities().instabuild = true;
        kiln.getBlockState().useItemOn(charge, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(charge.getCount() == 1 && kiln.getBurnTicks() > 0, "Creative ignition consumed an item");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void airAndFuelNeverIgniteCrucibleAndRelightingRetainsPaidBatch(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace = h.getBlockEntity(pos);
        furnace.getItemHandler(null).insertItem(0, new ItemStack(Items.RAW_COPPER, 3), false);
        furnace.getItemHandler(null).insertItem(1, new ItemStack(ModItems.CASTING_MOLD.get()), false);
        furnace.insertFuel(new ItemStack(Items.CHARCOAL), false);
        furnace.stoke(200);
        tick(h, furnace, 5);
        h.assertTrue(furnace.getProgress() == 0 && !furnace.isHot() && furnace.getFuel().getCount() == 1,
                "Bellows and fuel magically ignited crucible");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var tool = new ItemStack(Items.FLINT_AND_STEEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var hit = new BlockHitResult(Vec3.atCenterOf(furnace.getBlockPos()), Direction.NORTH, furnace.getBlockPos(), false);
        furnace.getBlockState().useItemOn(tool, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        tick(h, furnace, 2);
        h.assertTrue(furnace.getProgress() == 2 && furnace.getFuel().isEmpty() && furnace.isHot(), "Explicit crucible ignition failed");
        var saved = furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putInt("StokeTicks", 1);
        furnace.loadWithComponents(saved, h.getLevel().registryAccess());
        tick(h, furnace, 1);
        furnace.stoke(200);
        tick(h, furnace, 2);
        h.assertTrue(furnace.getProgress() == 2 && !furnace.isHot(), "Air alone relit extinguished crucible");
        furnace.getBlockState().useItemOn(tool, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        tick(h, furnace, 1);
        h.assertTrue(furnace.getProgress() == 3 && furnace.getFuel().isEmpty() && tool.getDamageValue() == 2,
                "Relighting lost paid batch or charged duplicate fuel");
        h.succeed();
    }

    private static void tick(GameTestHelper h, WorkshopBlockEntity station, int count) {
        for (int i = 0; i < count; i++) WorkshopBlockEntity.serverTick(h.getLevel(), station.getBlockPos(), station.getBlockState(), station);
    }
}
