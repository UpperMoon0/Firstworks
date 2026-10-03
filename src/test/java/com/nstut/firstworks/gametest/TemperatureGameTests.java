package com.nstut.firstworks.gametest;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.workshop.*;
import com.nstut.firstworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TemperatureGameTests {
    @GameTest(template="empty",timeoutTicks=45)
    public static void bellowsCostsFoodAndBoostsTemperatureInsteadOfWorkTime(GameTestHelper h) {
        var fp=new BlockPos(4,1,3);var bp=fp.west();
        h.setBlock(fp,ModBlocks.CRUCIBLE_FURNACE.get());
        h.setBlock(bp,ModBlocks.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.EAST));
        WorkshopBlockEntity furnace=h.getBlockEntity(fp);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        h.useBlock(bp,player);
        h.assertTrue(player.getFoodData().getFoodLevel()==20 && furnace.getStokeTicks()==0,"Cold failed blow charged hunger or added boost");
        furnace.getItemHandler(null).insertItem(0,new ItemStack(Items.RAW_COPPER,3),false);
        furnace.getItemHandler(null).insertItem(1,new ItemStack(ModItems.CASTING_MOLD.get()),false);
        furnace.insertFuel(new ItemStack(Items.COAL),false);
        furnace.ignite();
        tick(h,furnace,100);
        h.assertTrue(furnace.getTemperature()==120,"Crucible warmup still jumps to maximum in five seconds");
        tick(h,furnace,680);
        h.assertTrue(furnace.getTemperature()==800 && furnace.getProgress()==0,"Unassisted fuel melted copper");
        h.useBlock(bp,player);
        h.assertTrue(player.getFoodData().getFoodLevel()==19 && furnace.getMaxTemperature()==1150 && furnace.getTemperature()==800,"Blow did not charge once or instantly added heat");
        h.useBlock(bp,player);
        h.assertTrue(player.getFoodData().getFoodLevel()==19,"Cooldown click charged hunger");
        tick(h,furnace,300);
        h.assertTrue(furnace.getTemperature()>=1085 && furnace.getProgress()>0,"Boosted furnace could not process hot charge");
        int progress=furnace.getProgress();
        var decaying=furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        decaying.putInt("StokeTicks",301);furnace.loadWithComponents(decaying,h.getLevel().registryAccess());
        tick(h,furnace,301);
        h.assertTrue(furnace.getMaxTemperature()==800 && furnace.getTemperature()<=800 && furnace.getProgress()>progress,"Ceiling did not fall and clamp current heat");
        progress=furnace.getProgress();tick(h,furnace,10);
        h.assertTrue(furnace.getProgress()==progress && furnace.isHot(),"Below-threshold work advanced or fire extinguished");
        var saved=furnace.saveWithoutMetadata(h.getLevel().registryAccess());
        double temperature=furnace.getTemperature();int burn=furnace.getBurnTicks();
        furnace.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(furnace.getTemperature()==temperature && furnace.getBurnTicks()==burn,"Reload changed temperature or burn time");
        h.runAtTickTime(22,()->{
            player.getFoodData().setFoodLevel(0);h.useBlock(bp,player);
            h.assertTrue(furnace.getStokeTicks()==0,"Hungry player obtained free boost");
            player.getFoodData().setFoodLevel(18);h.useBlock(bp,player);
            h.assertTrue(player.getFoodData().getFoodLevel()==17 && furnace.getStokeTicks()>0,"Fresh blow did not resume boost after cooldown");
        });
        h.runAtTickTime(43,()->{
            player.getAbilities().instabuild=true;
            int food=player.getFoodData().getFoodLevel();h.useBlock(bp,player);
            h.assertTrue(player.getFoodData().getFoodLevel()==food,"Creative blow charged food");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void transferredItemsKeepAbsoluteTemperatureAndForgeWindow(GameTestHelper h) {
        var item=new ItemStack(Items.COPPER_INGOT);
        ItemHeat.setTemperature(item,h.getLevel(),794,1200);
        h.assertTrue(Math.abs(ItemHeat.celsius(item,h.getLevel())-794)<0.01 && ItemHeat.workable(item,h.getLevel()),"Item temperature was inferred incorrectly");
        var encoded=item.save(h.getLevel().registryAccess());
        var decoded=ItemStack.parse(h.getLevel().registryAccess(),encoded).orElseThrow();
        h.assertTrue(decoded.get(ModDataComponents.HEAT.get()).equals(item.get(ModDataComponents.HEAT.get())),"Item serialization lost absolute temperature");
        ItemHeat.setTemperature(decoded,h.getLevel(),499,1200);
        h.assertTrue(!ItemHeat.workable(decoded,h.getLevel()) && ItemHeat.state(decoded,h.getLevel()).equals("warm"),"Old 25 percent threshold still controls forging");
        ItemHeat.setTemperature(decoded,h.getLevel(),500,1200);
        h.assertTrue(ItemHeat.workable(decoded,h.getLevel()),"Explicit workable minimum rejected equality");
        var legacy=new ItemHeat(600,1200,h.getLevel().getGameTime());
        item.set(ModDataComponents.HEAT.get(),legacy);
        h.assertTrue(Math.abs(ItemHeat.celsius(item,h.getLevel())-560)<0.01,"Legacy heat was not migrated using its original fraction");
        h.succeed();
    }
    private static void tick(GameTestHelper h,WorkshopBlockEntity furnace,int count) {
        for(int i=0;i<count;i++) WorkshopBlockEntity.serverTick(h.getLevel(),furnace.getBlockPos(),furnace.getBlockState(),furnace);
    }
}
