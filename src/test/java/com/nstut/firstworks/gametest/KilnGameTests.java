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
    @GameTest(template = "empty", timeoutTicks = 2500)
    public static void separatelyHeatedIngredientsForgeAndReheatAsOneWorkpiece(GameTestHelper h) {
        BlockPos firstPos = new BlockPos(2, 1, 2), secondPos = new BlockPos(4, 1, 2), anvilPos = new BlockPos(6, 1, 2);
        h.setBlock(firstPos, ModBlocks.KILN.get());
        h.setBlock(secondPos, ModBlocks.KILN.get());
        h.setBlock(anvilPos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity first = h.getBlockEntity(firstPos), second = h.getBlockEntity(secondPos), anvil = h.getBlockEntity(anvilPos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STONE_HAMMER.get()));
        for (WorkshopBlockEntity kiln : new WorkshopBlockEntity[]{first, second}) {
            h.assertTrue(kiln.insert(new ItemStack(Items.PRISMARINE_SHARD), false), "Kiln rejected batch ingredient");
            h.assertTrue(!kiln.insert(new ItemStack(Items.PRISMARINE_SHARD), false), "Kiln accepted loose stacked ingredients");
            kiln.getItemHandler(null).insertItem(2, new ItemStack(Items.COAL, 4), false);
        }
        h.assertTrue(first.ignite(), "First kiln failed to ignite");
        h.runAtTickTime(40, () -> h.assertTrue(second.ignite(), "Second kiln failed to ignite"));
        h.runAtTickTime(605, () -> {
            ItemStack a = first.getItemHandler(null).extractItem(0, 1, false);
            ItemStack b = second.getItemHandler(null).extractItem(0, 1, false);
            h.assertTrue(!ItemStack.isSameItemSameComponents(a, b), "Fixture did not produce distinct heat components");
            h.assertTrue(anvil.insert(a, false), "Player insertion rejected first heated ingredient");
            double before = ItemHeat.celsius(anvil.getInput(), h.getLevel());
            double cooler = Math.min(before, ItemHeat.celsius(b, h.getLevel()));
            h.assertTrue(anvil.canInsert(b), "Player insertion rejected independently heated ingredient");
            var handler = anvil.getItemHandler(null);
            h.assertTrue(handler.insertItem(0, b, true).isEmpty(), "Automation simulation rejected heated batch assembly");
            h.assertTrue(anvil.getInput().getCount() == 1 && ItemHeat.celsius(anvil.getInput(), h.getLevel()) == before,
                    "Simulation changed stored count or heat");
            h.assertTrue(handler.insertItem(0, b, false).isEmpty(), "Automation rejected heated batch assembly");
            h.assertTrue(Math.abs(ItemHeat.celsius(anvil.getInput(), h.getLevel()) - cooler) < 0.001,
                    "Batch gained heat when combined");
            h.assertTrue(anvil.forge(player, "draw"), "Kiln-heated batch could not start forging");
        });
        h.runAtTickTime(1210, () -> {
            h.assertTrue(!ItemHeat.workable(anvil.getInput(), h.getLevel()), "Batch did not cool below its forging threshold");
            h.assertTrue(!anvil.forge(player, "bend"), "Cold batch could finish forging");
            ItemStack partial = anvil.getItemHandler(null).extractItem(0, 64, false);
            ItemStack incomplete = partial.copyWithCount(1);
            h.assertTrue(!first.insert(incomplete, false), "Kiln accepted a split worked batch");
            h.assertTrue(first.getItemHandler(null).insertItem(0, incomplete, false).getCount() == 1,
                    "Automation accepted a split worked batch");
            h.assertTrue(first.insert(partial, false) && partial.isEmpty(), "Player could not return complete batch to kiln");
            h.assertTrue(first.getItemHandler(null).getSlotLimit(0) == 2, "Occupied kiln did not expose worked-batch capacity");
            h.assertTrue(first.getItemHandler(null).extractItem(0, 1, false).isEmpty(), "Kiln split worked batch");
            var saved = first.saveWithoutMetadata(h.getLevel().registryAccess());
            first.loadWithComponents(saved, h.getLevel().registryAccess());
            ItemStack roundTrip = first.getItemHandler(null).extractItem(0, 2, false);
            h.assertTrue(first.getItemHandler(null).insertItem(0, roundTrip, true).isEmpty()
                    && first.getInput().isEmpty(), "Worked-batch insertion simulation mutated kiln");
            h.assertTrue(first.getItemHandler(null).insertItem(0, roundTrip, false).isEmpty(), "Automation could not return complete batch");
        });
        h.runAtTickTime(2415, () -> {
            ItemStack reheated = first.getItemHandler(null).extractItem(0, 64, false);
            h.assertTrue(reheated.getCount() == 2 && reheated.get(ModDataComponents.FORGE_PROGRESS.get()).completed() == 1,
                    "Reheating lost materials or forging progress");
            h.assertTrue(ItemHeat.fraction(reheated, h.getLevel()) > 0.95F, "Worked batch did not fully reheat");
            h.assertTrue(anvil.insert(reheated, false) && anvil.getProgress() == 1, "Batch did not resume its locked recipe");
            h.assertTrue(anvil.forge(player, "bend") && anvil.getOutput().is(Items.PRISMARINE_CRYSTALS)
                    && anvil.getInput().isEmpty(), "Reheated batch failed to finish with the correct yield");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void anvilAssemblyPreservesComponentsAndCannotWarmColdIngredients(GameTestHelper h) {
        BlockPos kilnPos = new BlockPos(2, 1, 2), anvilPos = new BlockPos(4, 1, 2);
        h.setBlock(kilnPos, ModBlocks.KILN.get());
        h.setBlock(anvilPos, ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity kiln = h.getBlockEntity(kilnPos), anvil = h.getBlockEntity(anvilPos);
        kiln.insert(new ItemStack(Items.PRISMARINE_SHARD), false);
        kiln.insertFuel(new ItemStack(Items.COAL), false);
        kiln.ignite();
        WorkshopBlockEntity.serverTick(h.getLevel(), kiln.getBlockPos(), kiln.getBlockState(), kiln);
        ItemStack warm = kiln.getItemHandler(null).extractItem(0, 1, false);
        h.assertTrue(ItemHeat.celsius(warm, h.getLevel()) > ThermalModel.AMBIENT, "Kiln did not warm fixture");
        anvil.insert(warm, false);
        ItemStack named = new ItemStack(Items.PRISMARINE_SHARD);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Different"));
        h.assertTrue(!anvil.canInsert(named) && !anvil.getItemHandler(null).insertItem(0, named, false).isEmpty(),
                "Assembly ignored non-heat components");
        h.assertTrue(anvil.insert(new ItemStack(Items.PRISMARINE_SHARD), false), "Player could not assemble different temperatures");
        h.assertTrue(anvil.getInput().getCount() == 2 && ItemHeat.celsius(anvil.getInput(), h.getLevel()) == ThermalModel.AMBIENT,
                "Warm ingredient gave cold ingredient free heat");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void coldWorkpiecesMergeThroughHopperWithoutLosingForgeState(GameTestHelper h) {
        BlockPos hopperPos = new BlockPos(3, 2, 3), chestPos = hopperPos.east();
        h.setBlock(hopperPos, net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, net.minecraft.core.Direction.EAST));
        h.setBlock(chestPos, net.minecraft.world.level.block.Blocks.CHEST);
        net.minecraft.world.level.block.entity.HopperBlockEntity hopper = h.getBlockEntity(hopperPos);
        net.minecraft.world.level.block.entity.ChestBlockEntity chest = h.getBlockEntity(chestPos);
        long now = h.getLevel().getGameTime();
        ItemStack stored = new ItemStack(Items.COPPER_INGOT, 63);
        stored.set(ModDataComponents.HEAT.get(), new ItemHeat(1, 1200, now - 10));
        ItemStack incoming = new ItemStack(Items.COPPER_INGOT);
        incoming.set(ModDataComponents.HEAT.get(), new ItemHeat(2, 1200, now - 20));
        chest.setItem(0, stored);
        for (int i = 1; i < chest.getContainerSize(); i++) chest.setItem(i, new ItemStack(Items.STICK, 64));
        hopper.setItem(0, incoming);
        ItemStack hot = new ItemStack(Items.COPPER_INGOT);
        ItemHeat.set(hot, h.getLevel(), 1200, 1200);
        h.assertTrue(!ItemStack.isSameItemSameComponents(hot, new ItemStack(Items.COPPER_INGOT)), "Hot and cold workpieces merged");
        h.assertTrue(hot.has(ModDataComponents.HEAT.get()), "Comparison erased live heat");
        ItemStack partial = new ItemStack(Items.PRISMARINE_SHARD, 2);
        var forge = new ForgeProgress("firstworks:gametest_heated_batch", "draw,bend", 1, 2);
        partial.set(ModDataComponents.HEAT.get(), new ItemHeat(1, 1200, now - 5));
        partial.set(ModDataComponents.FORGE_PROGRESS.get(), forge);
        h.assertTrue(!ItemStack.isSameItemSameComponents(partial, new ItemStack(Items.PRISMARINE_SHARD, 2)), "Normalization erased forging identity");
        h.assertTrue(partial.get(ModDataComponents.FORGE_PROGRESS.get()).equals(forge)
                && !partial.has(ModDataComponents.HEAT.get()), "Cold normalization lost forge progress or retained heat");
        h.runAtTickTime(12, () -> {
            h.assertTrue(hopper.isEmpty() && chest.getItem(0).getCount() == 64, "Cold timestamped stacks could not merge into a full chest");
            h.assertTrue(!chest.getItem(0).has(ModDataComponents.HEAT.get()), "Merged cold stack retained stale heat");
            h.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 660)
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
        h.assertTrue(kiln.ignite(), "Kiln ignition failed");
        h.runAtTickTime(105, () -> h.assertTrue(!ItemHeat.workable(kiln.getInput(), h.getLevel()), "Kiln reached forging temperature too quickly"));
        h.runAtTickTime(605, () -> {
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
        h.runAtTickTime(615, () -> {
            h.assertTrue(kiln.getInput().get(ModDataComponents.FORGE_PROGRESS.get()).completed() == 1, "Kiln advanced or erased anvil actions");
            var hot = kiln.getItemHandler(null).extractItem(0, 64, false);
            anvil.insert(hot, false);
            h.assertTrue(anvil.getProgress() == 1 && anvil.forge(player, "draw"), "Reheated work did not resume at next action");
            h.assertTrue(anvil.getBlockState().getValue(WorkshopBlock.HEAT_LIGHT) >= 11, "Hot anvil workpiece emitted no light");
        });
        h.runAtTickTime(620, () -> {
            h.assertTrue(h.getLevel().getBrightness(LightLayer.BLOCK, anvil.getBlockPos().above()) > 0, "Light engine did not illuminate anvil surroundings");
            ItemHeat.set(anvil.getInput(), h.getLevel(), 3, 1200);
        });
        h.runAtTickTime(626, () -> {
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
        h.assertTrue(kiln.getBurnTicks() == 0 && kiln.getFuel().getCount() == 1, "Fuel lit itself");
        h.assertTrue(kiln.ignite(), "Kiln ignition failed");
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
