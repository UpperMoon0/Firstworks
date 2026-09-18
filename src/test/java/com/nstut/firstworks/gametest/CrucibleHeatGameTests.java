package com.nstut.firstworks.gametest;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrucibleHeatGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void heatTracksFuelAndAirRatherThanRetainedProgress(GameTestHelper h) {
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace = h.getBlockEntity(pos);
        furnace.getItemHandler(null).insertItem(0, new ItemStack(Items.RAW_COPPER, 3), false);
        furnace.getItemHandler(null).insertItem(1, new ItemStack(ModItems.CASTING_MOLD.get()), false);
        furnace.stoke(10);
        tick(h, pos, furnace, 2);
        h.assertTrue(!furnace.isHot() && furnace.getProgress() == 0, "Air without fuel appeared hot");
        furnace.insertFuel(new ItemStack(Items.CHARCOAL), false);
        tick(h, pos, furnace, 1);
        h.assertTrue(furnace.isHot() && furnace.getFuel().isEmpty(), "Paid processing did not appear hot");
        tick(h, pos, furnace, 8);
        int savedProgress = furnace.getProgress();
        h.assertTrue(savedProgress > 0 && !furnace.isHot(), "Retained progress kept crucible glowing after air loss");
        var saved = furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        furnace.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(!furnace.isHot() && furnace.getProgress() == savedProgress, "Reload restored stuck glow or lost progress");
        furnace.stoke(400);
        tick(h, pos, furnace, 1);
        h.assertTrue(furnace.isHot() && furnace.getProgress() == savedProgress + 1 && furnace.getFuel().isEmpty(), "Resume required duplicate fuel or lost heat/progress");
        tick(h, pos, furnace, 300);
        h.assertTrue(!furnace.isHot() && furnace.getOutput().is(ModItems.CAST_COPPER_BILLET.get()), "Completed output stayed hot");
        h.assertTrue(furnace.getCatalyst().is(ModItems.CASTING_MOLD.get()), "Heat fix consumed reusable mold");
        furnace.getItemHandler(null).extractItem(3, 64, false);
        h.assertTrue(!furnace.isHot(), "Output extraction revived heat");
        furnace.getItemHandler(null).insertItem(0, new ItemStack(Items.RAW_COPPER, 3), false);
        tick(h, pos, furnace, 1);
        h.assertTrue(!furnace.isHot() && furnace.getProgress() == 0, "Next unfueled batch appeared hot");
        h.succeed();
    }
    private static void tick(GameTestHelper h, BlockPos pos, WorkshopBlockEntity furnace, int ticks) {
        for (int i = 0; i < ticks; i++) WorkshopBlockEntity.serverTick(h.getLevel(), h.absolutePos(pos), furnace.getBlockState(), furnace);
    }
}
