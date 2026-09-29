package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.*;
import com.nstut.firstworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KilnGameTests {
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void heatTransferResumeAndLightFade(GameTestHelper h) {
        BlockPos kp = new BlockPos(3, 1, 3), ap = new BlockPos(5, 1, 3);
        h.setBlock(kp, ModBlocks.KILN.get());
        h.setBlock(ap, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity kiln = h.getBlockEntity(kp), anvil = h.getBlockEntity(ap);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        h.assertTrue(!kiln.insert(new ItemStack(ModItems.UNFIRED_REFRACTORY_BRICK.get()), false), "Kiln accepted pottery for firing");
        h.assertTrue(kiln.insert(new ItemStack(ModItems.ANNEALED_COPPER_BILLET.get()), false), "Kiln rejected billet");
        WorkshopBlockEntity.serverTick(h.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
        h.assertTrue(kiln.getForgeHeat() == 0, "Kiln heated without fuel");
        h.assertTrue(kiln.insertFuel(new ItemStack(Items.CHARCOAL), false), "Kiln rejected fuel");
        h.runAtTickTime(105, () -> {
            h.assertTrue(ItemHeat.fraction(kiln.getInput(), h.getLevel()) > 0.95F, "Billet did not heat fully");
            h.assertTrue(kiln.getOutput().isEmpty() && kiln.getProgress() == 0, "Kiln transformed or worked the item");
            h.assertTrue(kiln.getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) == 12, "Kiln heat emitted no light");
            var hot = kiln.getItemHandler(null).extractItem(0, 64, false);
            var decoded = ItemStack.parse(h.getLevel().registryAccess(), hot.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(decoded.get(ModDataComponents.HEAT.get()).equals(hot.get(ModDataComponents.HEAT.get())), "Saved item lost heat");
            h.assertTrue(anvil.insert(decoded, false), "Anvil rejected heated item");
            h.assertTrue(anvil.forge(player, "flatten"), "Hot item could not be forged");
            var partial = anvil.getItemHandler(null).extractItem(0, 64, false);
            h.assertTrue(partial.get(ModDataComponents.FORGE_PROGRESS.get()).completed() == 1, "Extracted item lost progress");
            h.assertTrue(anvil.getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) == 0, "Empty anvil kept emitting light");
            h.assertTrue(kiln.insert(partial, false), "Kiln rejected partially forged item");
        });
        h.runAtTickTime(115, () -> {
            h.assertTrue(kiln.getInput().get(ModDataComponents.FORGE_PROGRESS.get()).completed() == 1, "Kiln advanced or erased anvil actions");
            var hot = kiln.getItemHandler(null).extractItem(0, 64, false);
            anvil.insert(hot, false);
            h.assertTrue(anvil.getProgress() == 1 && anvil.forge(player, "draw"), "Reheated work did not resume at next action");
            h.assertTrue(anvil.getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) >= 11, "Hot anvil workpiece emitted no light");
        });
        h.runAtTickTime(120, () -> {
            h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, anvil.getBlockPos().above()) > 0, "Light engine did not illuminate anvil surroundings");
            ItemHeat.set(anvil.getInput(), h.getLevel(), 3, 1200);
        });
        h.runAtTickTime(126, () -> {
            h.assertTrue(anvil.getForgeHeat() == 0 && anvil.getProgress() == 2, "Cooling lost work or retained heat");
            h.assertTrue(!anvil.forge(player, "bend"), "Cold workpiece was forged");
            h.assertTrue(anvil.getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) == 0, "Cold anvil still emitted light");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void splitBatchesOnlyResumeWithAllMaterials(GameTestHelper h) {
        BlockPos first = new BlockPos(3, 1, 3), second = new BlockPos(5, 1, 3);
        h.setBlock(first, ModBlocks.STONE_ANVIL.get());
        h.setBlock(second, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity a = h.getBlockEntity(first), b = h.getBlockEntity(second);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        var stack = new ItemStack(Items.PRISMARINE_SHARD, 2);
        ItemHeat.set(stack, h.getLevel(), 1200, 1200);
        h.assertTrue(a.getItemHandler(null).insertItem(0, stack, false).isEmpty(), "Batch rejected");
        h.assertTrue(a.forge(player, "draw"), "Batch could not begin");
        h.assertTrue(a.getItemHandler(null).extractItem(0, 1, false).isEmpty(), "Workshop split an unfinished batch");
        var partial = a.getItemHandler(null).extractItem(0, 2, false);
        var one = partial.copyWithCount(1);
        b.getItemHandler(null).insertItem(0, one, false);
        h.assertTrue(b.getProgress() == 0 && !b.forge(player, "bend"), "Half a batch duplicated completed work");
        b.getItemHandler(null).insertItem(0, one, false);
        h.assertTrue(b.getProgress() == 1 && b.forge(player, "bend"), "Reassembled batch did not resume");
        h.assertTrue(b.getOutput().is(Items.PRISMARINE_CRYSTALS) && b.getInput().isEmpty(), "Batch did not consume all materials");
        h.assertTrue(ItemHeat.remaining(b.getOutput(), h.getLevel()) > 0, "Finished work lost heat");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fuelBurnAndSavedHeatCannotCreateRecipeOutputs(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.KILN.get());
        WorkshopBlockEntity kiln = h.getBlockEntity(pos);
        kiln.insert(new ItemStack(Items.COPPER_INGOT), false);
        kiln.insertFuel(new ItemStack(Items.STICK), false);
        WorkshopBlockEntity.serverTick(h.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
        int burn = kiln.getBurnTicks();
        var nbt = kiln.saveWithoutMetadata(h.getLevel().registryAccess());
        kiln.loadWithComponents(nbt, h.getLevel().registryAccess());
        h.assertTrue(kiln.getBurnTicks() == burn && kiln.getForgeHeat() > 0, "Reload lost burn or item heat");
        for (int i = 0; i < burn + 1; i++) WorkshopBlockEntity.serverTick(h.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
        h.assertTrue(kiln.getBurnTicks() == 0 && kiln.getFuel().isEmpty(), "Fuel burned indefinitely");
        h.assertTrue(kiln.getInput().is(Items.COPPER_INGOT) && kiln.getOutput().isEmpty() && kiln.activeRecipe().isEmpty(), "Kiln smelted the input");
        h.succeed();
    }
}
