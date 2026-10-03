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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContinuousFireGameTests {
    @GameTest(template="empty", timeoutTicks=20)
    public static void kilnInputIsOneWhileFuelStacks(GameTestHelper h) {
        h.setBlock(new BlockPos(3,1,3), ModBlocks.KILN.get());
        WorkshopBlockEntity kiln=h.getBlockEntity(new BlockPos(3,1,3));
        var handler=kiln.getItemHandler(null);
        ItemStack copper=new ItemStack(Items.COPPER_INGOT,64);
        h.assertTrue(handler.getSlotLimit(0)==1 && handler.getSlotLimit(2)==64,"Wrong slot limits");
        h.assertTrue(handler.insertItem(0,copper,true).getCount()==63 && kiln.getInput().isEmpty(),"Simulation changed input or accepted a stack");
        h.assertTrue(handler.insertItem(0,copper,false).getCount()==63 && kiln.getInput().getCount()==1,"Automation accepted more than one workpiece");
        h.assertTrue(!kiln.insert(copper,false) && copper.getCount()==64,"Player stacked workpieces");
        h.assertTrue(handler.insertItem(2,new ItemStack(Items.COAL,64),false).isEmpty() && kiln.getFuel().getCount()==64,"Fuel stopped stacking");
        handler.extractItem(0,1,false);
        h.assertTrue(kiln.insert(copper,false) && copper.getCount()==63 && kiln.getInput().getCount()==1,"Player insertion did not take exactly one");
        handler.extractItem(0,1,false);
        var batch=new ItemStack(Items.PRISMARINE_SHARD,2);
        batch.set(ModDataComponents.FORGE_PROGRESS.get(),new ForgeProgress("firstworks:gametest_heated_batch","draw,bend",1,2));
        h.assertTrue(kiln.insert(batch,false) && batch.isEmpty() && kiln.getInput().getCount()==2,"Kiln failed to accept a complete worked batch");
        h.assertTrue(handler.extractItem(0,1,false).isEmpty(),"Kiln split a saved forging batch");
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=20)
    public static void crucibleFireSurvivesRecipesOutputAndReload(GameTestHelper h) {
        h.setBlock(new BlockPos(3,1,3), ModBlocks.CRUCIBLE_FURNACE.get());
        WorkshopBlockEntity furnace=h.getBlockEntity(new BlockPos(3,1,3));
        var handler=furnace.getItemHandler(null);
        handler.insertItem(0,new ItemStack(Items.RAW_COPPER,3),false);
        handler.insertItem(1,new ItemStack(ModItems.CASTING_MOLD.get()),false);
        handler.insertItem(2,new ItemStack(Items.STICK,3),false);
        int duration=new ItemStack(Items.STICK).getBurnTime(RecipeType.SMELTING);
        h.assertTrue(furnace.ignite() && furnace.getBurnTicks()==duration && furnace.getFuel().getCount()==2,"Ignition did not use native stick burn time");
        furnace.stoke(600);
        tick(h,furnace,240);
        h.assertTrue(furnace.getOutput().is(ModItems.CAST_COPPER_BILLET.get()) && furnace.isHot() && !furnace.needsIgnition(),"Recipe completion extinguished the fire");
        int remaining=furnace.getBurnTicks();
        tick(h,furnace,5);
        h.assertTrue(furnace.getBurnTicks()==remaining-5,"Waiting output stopped fuel burn");
        h.assertTrue(handler.insertItem(2,new ItemStack(Items.STICK,3),false).isEmpty(),"Ready output blocked fuel refill");
        var saved=furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        remaining=furnace.getBurnTicks();
        furnace.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(furnace.getBurnTicks()==remaining,"Reload reset burn time");
        handler.extractItem(3,64,false);
        handler.insertItem(0,new ItemStack(Items.RAW_COPPER,3),false);
        tick(h,furnace,240);
        h.assertTrue(furnace.getOutput().is(ModItems.CAST_COPPER_BILLET.get()) && furnace.isHot(),"Second batch required ignition");
        handler.extractItem(3,64,false);
        tick(h,furnace,furnace.getBurnTicks()+furnace.getFuel().getCount()*duration);
        h.assertTrue(!furnace.isHot() && furnace.getBurnTicks()==0 && furnace.getFuel().isEmpty(),"Fire outlasted its native fuel duration");
        handler.insertItem(2,new ItemStack(Items.STICK),false);
        tick(h,furnace,2);
        h.assertTrue(!furnace.isHot() && furnace.needsIgnition() && furnace.getFuel().getCount()==1,"Cold refill magically lit itself");
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=20)
    public static void nativeFuelRemaindersAndExactBurnout(GameTestHelper h) {
        for(int x:new int[]{3,5}) {
            h.setBlock(new BlockPos(x,1,3),x==3?ModBlocks.KILN.get():ModBlocks.CRUCIBLE_FURNACE.get());
            WorkshopBlockEntity station=h.getBlockEntity(new BlockPos(x,1,3));
            station.insertFuel(new ItemStack(Items.STICK),false);
            h.assertTrue(station.ignite(),"Empty station could not be ignited");
            int duration=new ItemStack(Items.STICK).getBurnTime(RecipeType.SMELTING);
            tick(h,station,duration-1);
            h.assertTrue(station.getBurnTicks()==1 && station.isHot(),"Fuel expired early");
            tick(h,station,1);
            h.assertTrue(station.getBurnTicks()==0 && !station.isHot(),"Fuel burned past native duration");
            station.insertFuel(new ItemStack(Items.LAVA_BUCKET),false);
            h.assertTrue(station.ignite() && station.getBurnTicks()==new ItemStack(Items.LAVA_BUCKET).getBurnTime(RecipeType.SMELTING)
                    && station.getFuel().is(Items.BUCKET),"Native fuel rejected or bucket remainder lost");
        }
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=30)
    public static void fastenersRequireHeatedAnvilForging(GameTestHelper h) {
        h.setBlock(new BlockPos(3,1,3),ModBlocks.STONE_ANVIL.get());
        WorkshopBlockEntity anvil=h.getBlockEntity(new BlockPos(3,1,3));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.STONE_HAMMER.get()));
        anvil.insert(new ItemStack(Items.COPPER_INGOT),false);
        h.assertTrue(!anvil.forge(player,"flatten"),"Cold copper made fasteners");
        ItemHeat.set(anvil.getInput(),h.getLevel(),1200,1200);
        h.assertTrue(anvil.forge(player,"flatten"),"Hot copper failed first action");
        h.runAtTickTime(6,()->h.assertTrue(anvil.forge(player,"draw"),"Draw failed"));
        h.runAtTickTime(12,()->{
            h.assertTrue(anvil.forge(player,"bend"),"Bend failed");
            h.assertTrue(anvil.getInput().isEmpty() && anvil.getOutput().is(ModItems.COPPER_FASTENERS.get()) && anvil.getOutput().getCount()==8,"Fastener forging produced wrong output");
            h.succeed();
        });
    }
    private static void tick(GameTestHelper h,WorkshopBlockEntity station,int count) {
        ThermalTestSupport.tickHot(h.getLevel(),station.getBlockPos(),station,count);
    }
}
