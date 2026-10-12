package com.nstut.firstworks.gametest;

import com.nstut.firstworks.*;
import com.nstut.firstworks.content.workshop.*;
import com.nstut.firstworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.*;
import java.util.Objects;

@GameTestHolder(Firstworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoolingGameTests {
    private static final BlockPos POS=new BlockPos(3,1,3);
    private static CoolingProfiles.Selected water() { return CoolingProfiles.select(Fluids.WATER.defaultFluidState()); }
    @GameTest(template="empty",timeoutTicks=35)
    public static void switchingMediaAndDuplicateObservationsNeverRestoreHeat(GameTestHelper h) {
        var stack=new ItemStack(Items.COPPER_INGOT);
        ItemHeat.setTemperature(stack,h.getLevel(),1000,3600);
        CoolingSystem.observe(stack,h.getLevel(),h.absolutePos(POS),water());
        double rate=ItemHeat.coolingRate(stack);
        h.assertTrue(Math.abs(rate-3)<0.0001,"Water rate is not ten times ordinary copper cooling");
        var saved=stack.save(h.getLevel().registryAccess());
        var restored=ItemStack.parse(h.getLevel().registryAccess(),saved).orElseThrow();
        h.runAtTickTime(10,()->{
            double before=ItemHeat.celsius(restored,h.getLevel());
            CoolingSystem.observe(restored,h.getLevel(),h.absolutePos(POS),CoolingProfiles.air());
            h.assertTrue(Math.abs(ItemHeat.celsius(restored,h.getLevel())-before)<0.0001,"Leaving water restored heat");
            for(int i=0;i<30;i++) { CoolingSystem.observe(restored,h.getLevel(),h.absolutePos(POS),water());CoolingSystem.observe(restored,h.getLevel(),h.absolutePos(POS),CoolingProfiles.air()); }
            h.assertTrue(Math.abs(ItemHeat.celsius(restored,h.getLevel())-before)<0.0001,"Repeated clicks minted cooling or heat");
        });
        h.runAtTickTime(20,()->{h.assertTrue(ItemHeat.celsius(restored,h.getLevel())<970 && ItemHeat.celsius(restored,h.getLevel())>950,"Air reverted to old timeline or retained water rate");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void droppedWaterAndFilledCauldronCoolButEmptyCauldronDoesNot(GameTestHelper h) {
        var wp=new BlockPos(2,1,2);var cp=new BlockPos(4,1,2);var ep=new BlockPos(6,1,2);
        h.setBlock(wp,Blocks.WATER);h.setBlock(cp,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));h.setBlock(ep,Blocks.CAULDRON);
        ItemEntity[] entities=new ItemEntity[3];BlockPos[] positions={wp,cp,ep};
        for(int i=0;i<3;i++) {
            var absolute=h.absolutePos(positions[i]);var stack=new ItemStack(Items.COPPER_INGOT);ItemHeat.setTemperature(stack,h.getLevel(),1000,3600);
            var item=new ItemEntity(h.getLevel(),absolute.getX()+0.5,absolute.getY()+0.4,absolute.getZ()+0.5,stack);
            item.setNoGravity(true);item.setDeltaMovement(0,0,0);h.getLevel().addFreshEntity(item);entities[i]=item;
        }
        h.runAtTickTime(20,()->{
            double wet=ItemHeat.celsius(entities[0].getItem(),h.getLevel()),cauldron=ItemHeat.celsius(entities[1].getItem(),h.getLevel()),dry=ItemHeat.celsius(entities[2].getItem(),h.getLevel());
            h.assertTrue(wet<970 && cauldron<970 && dry>980,"Dropped-item immersion was not limited to water contact");
            h.assertTrue(h.getBlockState(cp).getValue(LayeredCauldronBlock.LEVEL)==3,"Physical cooling consumed water");
            for(var item:entities)item.discard();h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void protectionBlocksManualCollectionAndPickupButAllowsAutomation(GameTestHelper h) {
        h.setBlock(POS,ModBlocks.KILN.get());WorkshopBlockEntity station=h.getBlockEntity(POS);
        var hot=new ItemStack(Items.COPPER_INGOT);ItemHeat.setTemperature(hot,h.getLevel(),900,3600);station.insert(hot,false);
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(station.takeStored(p) && !station.getInput().isEmpty(),"Unprotected collection removed a hot stack");
        var item=new ItemEntity(h.getLevel(),0,0,0,station.getInput().copy());
        var pickup=new ItemEntityPickupEvent.Pre(p,item);NeoForge.EVENT_BUS.post(pickup);
        h.assertTrue(pickup.canPickup()==TriState.FALSE,"Unprotected pickup was allowed");
        var simulated=station.getItemHandler(null).extractItem(0,1,true);
        h.assertTrue(!simulated.isEmpty() && !station.getInput().isEmpty(),"Automation simulation changed or rejected hot input");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.PRIMITIVE_TONGS.get()));
        h.assertTrue(station.takeStored(p) && station.getInput().isEmpty(),"Offhand tongs failed to collect");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        p.getInventory().add(new ItemStack(ModItems.PRIMITIVE_TONGS.get()));
        h.assertTrue(!HeatProtection.allow(p,item.getItem(),InteractionHand.MAIN_HAND),"Inventory tongs counted as equipped");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PRIMITIVE_TONGS.get()));
        h.assertTrue(HeatProtection.allow(p,item.getItem(),InteractionHand.OFF_HAND),"Mainhand protection failed for an offhand transaction");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ModItems.PRIMITIVE_TONGS.get()));
        h.assertTrue(HeatProtection.allow(p,item.getItem(),InteractionHand.MAIN_HAND),"Tagged worn protection failed");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,ItemStack.EMPTY);
        p.getAbilities().instabuild=true;
        h.assertTrue(HeatProtection.allow(p,item.getItem(),InteractionHand.MAIN_HAND),"Creative exemption failed");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void explicitProtectionAndAmbientOverridesRemainRespected(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var hot=new ItemStack(Items.COPPER_INGOT);ItemHeat.setTemperature(hot,h.getLevel(),900,3600);
        boolean protection=FirstworksConfig.REQUIRE_HEAT_PROTECTION.get(),ambient=FirstworksConfig.ENABLE_AMBIENT_COOLING.get();
        try {
            FirstworksConfig.REQUIRE_HEAT_PROTECTION.set(false);FirstworksConfig.ENABLE_AMBIENT_COOLING.set(false);
            h.assertTrue(HeatProtection.allow(p,hot,InteractionHand.MAIN_HAND),"Explicit protection=false ignored");
            h.assertTrue(CoolingSystem.ambientMultiplier(h.getLevel(),h.absolutePos(POS))==1,"Explicit ambient=false ignored");
        } finally { FirstworksConfig.REQUIRE_HEAT_PROTECTION.set(protection);FirstworksConfig.ENABLE_AMBIENT_COOLING.set(ambient); }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void treatmentsLatchConvertOnceAndKeepUnrelatedComponents(GameTestHelper h) {
        var stack=new ItemStack(Items.IRON_NUGGET,4);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Paid workpiece"));
        ItemHeat.setTemperature(stack,h.getLevel(),1000,100);
        CoolingSystem.observe(stack,h.getLevel(),h.absolutePos(POS),water());
        h.assertTrue(stack.get(ModDataComponents.COOLING_TREATMENT.get()).recipe().equals("firstworks:gametest_cooling_priority_a"),"Treatment priority/ID ordering failed");
        var encoded=stack.save(h.getLevel().registryAccess());var restored=ItemStack.parse(h.getLevel().registryAccess(),encoded).orElseThrow();
        // Selection survives leaving the entry medium; completing the thermal timeline still fires once.
        h.runAtTickTime(2,()->CoolingSystem.observe(restored,h.getLevel(),h.absolutePos(POS),CoolingProfiles.air()));
        h.runAtTickTime(8,()->{
            var converted=CoolingSystem.observe(restored,h.getLevel(),h.absolutePos(POS),water());
            h.assertTrue(converted.is(Items.GOLD_NUGGET) && converted.getCount()==2,"Treatment count conversion failed");
            h.assertTrue(Objects.equals(converted.get(DataComponents.CUSTOM_NAME),Component.literal("Paid workpiece")),"Treatment erased unrelated name");
            h.assertTrue("quenched".equals(converted.get(ModDataComponents.THERMAL_MARKER.get())),"Persistent marker missing");
            for(int i=0;i<10;i++)converted=CoolingSystem.observe(converted,h.getLevel(),h.absolutePos(POS),water());
            h.assertTrue(converted.getCount()==2,"Repeated observation duplicated conversion");
            double before=ItemHeat.celsius(converted,h.getLevel());ItemHeat.setTemperature(converted,h.getLevel(),1000,100);
            h.assertTrue(ItemHeat.celsius(converted,h.getLevel())==before,"Permanent treatment restriction allowed reheating");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void missingTreatmentAndIncompatibleForgeProgressPreserveItems(GameTestHelper h) {
        var stack=new ItemStack(Items.IRON_NUGGET,4);ItemHeat.setTemperature(stack,h.getLevel(),700,100);
        stack.set(ModDataComponents.COOLING_TREATMENT.get(),new CoolingTreatmentState("firstworks:water","firstworks:deleted_recipe",800,false));
        var result=CoolingSystem.observe(stack,h.getLevel(),h.absolutePos(POS),water());
        h.assertTrue(result.is(Items.IRON_NUGGET) && result.getCount()==4 && result.get(ModDataComponents.COOLING_TREATMENT.get()).finished(),"Missing recipe changed or reselected treatment");
        var forged=new ItemStack(Items.IRON_NUGGET,4);var progress=new ForgeProgress("firstworks:example","draw",1,4);forged.set(ModDataComponents.FORGE_PROGRESS.get(),progress);ItemHeat.setTemperature(forged,h.getLevel(),1000,100);
        CoolingSystem.observe(forged,h.getLevel(),h.absolutePos(POS),water());
        h.assertTrue(forged.get(ModDataComponents.COOLING_TREATMENT.get()).recipe().isEmpty(),"Incompatible partly forged item selected transformation");
        h.assertTrue(forged.get(ModDataComponents.FORGE_PROGRESS.get()).equals(progress),"Cooling lost forging progress");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void lavaCannotQuenchAndDimensionTransitionsDoNotRestoreHeat(GameTestHelper h) {
        h.assertTrue(CoolingProfiles.select(Fluids.LAVA.defaultFluidState()).profile().medium().equals("air"),"Lava counted as coolant");
        var stack=new ItemStack(Items.COPPER_INGOT);var now=h.getLevel().getGameTime();
        stack.set(ModDataComponents.HEAT.get(),new ItemHeat(3000,3600,now-1000,500,1100,10,20,"minecraft:the_nether"));
        h.assertTrue(ItemHeat.celsius(stack,h.getLevel())==500,"Destination time was applied to departure state");
        ItemHeat.profile(stack,h.getLevel(),1,20);
        h.assertTrue(ItemHeat.celsius(stack,h.getLevel())==500,"Adopting destination profile changed temperature");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void taggedShearsAndCancelledDualTaggedInteractionsAreSingleTransactions(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);var sheep=new Sheep(EntityType.SHEEP,h.getLevel());sheep.setAge(0);
        var shears=new ItemStack(Items.WOODEN_HOE);p.setItemInHand(InteractionHand.OFF_HAND,shears);
        var e=new PlayerInteractEvent.EntityInteract(p,InteractionHand.OFF_HAND,sheep);NeoForge.EVENT_BUS.post(e);
        h.assertTrue(e.isCanceled() && sheep.isSheared() && shears.getDamageValue()==1,"Tag-only modded tool was not sheared exactly once in the correct hand");
        var dual=new Sheep(EntityType.SHEEP,h.getLevel());dual.setAge(0);var tool=new ItemStack(ModItems.FLINT_KNIFE.get());p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var d=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,dual);NeoForge.EVENT_BUS.post(d);
        h.assertTrue(d.isCanceled() && dual.isSheared() && tool.getDamageValue()==8,"Dual-tag knife/shears duplicated damage");
        var cancelled=new Sheep(EntityType.SHEEP,h.getLevel());cancelled.setAge(0);var c=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,cancelled);c.setCanceled(true);
        GameplayEvents.knifeShearFleece(c);GameplayEvents.shearFleece(c);
        h.assertTrue(!cancelled.isSheared() && tool.getDamageValue()==8,"Cancelled interaction was processed");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void heldQuenchStartsInTheActualHandAndStopsWithoutExtraCooling(GameTestHelper h) {
        h.setBlock(POS,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
        var absolute=h.absolutePos(POS);var p=h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(absolute.getX()+0.5,absolute.getY()+1.1,absolute.getZ()+0.5);p.setXRot(90);
        var stack=new ItemStack(Items.COPPER_INGOT);ItemHeat.setTemperature(stack,h.getLevel(),1000,3600);p.setItemInHand(InteractionHand.OFF_HAND,stack);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),net.minecraft.core.Direction.UP,absolute,false);
        var event=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.OFF_HAND,absolute,hit);CoolingEvents.startQuench(event);
        h.assertTrue(event.isCanceled() && p.isUsingItem() && p.getUsedItemHand()==InteractionHand.OFF_HAND,"Quench did not start with the actual hand");
        var tick=new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick(p,stack,72000);CoolingEvents.useTick(tick);
        h.assertTrue(ItemHeat.coolingRate(stack)>2.9,"Held cauldron use did not adopt water profile");
        h.runAtTickTime(10,()->{
            double before=ItemHeat.celsius(stack,h.getLevel());
            CoolingEvents.stopQuench(new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Stop(p,stack,71990));
            h.assertTrue(!p.isUsingItem() && !CoolingEvents.isQuenching(p),"Release did not stop held quenching");
            h.assertTrue(ItemHeat.celsius(stack,h.getLevel())==before && ItemHeat.coolingRate(stack)<1,"Release restored heat or retained water cooling");
            h.assertTrue(h.getBlockState(POS).getValue(LayeredCauldronBlock.LEVEL)==3,"Held cooling consumed cauldron water");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void crucibleNonePolicyLeavesEligibleOutputCold(GameTestHelper h) {
        h.setBlock(POS,ModBlocks.CRUCIBLE_FURNACE.get());WorkshopBlockEntity furnace=h.getBlockEntity(POS);
        furnace.getItemHandler(null).insertItem(0,new ItemStack(Items.RAW_GOLD),false);furnace.insertFuel(new ItemStack(Items.COAL),false);furnace.ignite();
        for(int i=0;i<5;i++) WorkshopBlockEntity.serverTick(h.getLevel(),furnace.getBlockPos(),furnace.getBlockState(),furnace);
        h.assertTrue(furnace.getOutput().is(Items.COPPER_INGOT) && !furnace.getOutput().has(ModDataComponents.HEAT.get()),"result_heat=none heated an eligible custom output");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=20)
    public static void crucibleOutputsUseStationHeatAndSimulationPreservesIt(GameTestHelper h) {
        h.setBlock(POS,ModBlocks.CRUCIBLE_FURNACE.get());WorkshopBlockEntity furnace=h.getBlockEntity(POS);
        furnace.getItemHandler(null).insertItem(0,new ItemStack(Items.RAW_COPPER,3),false);furnace.getItemHandler(null).insertItem(1,new ItemStack(ModItems.CASTING_MOLD.get()),false);furnace.insertFuel(new ItemStack(Items.COAL),false);furnace.ignite();
        ThermalTestSupport.tickHot(h.getLevel(),furnace.getBlockPos(),furnace,300);
        h.assertTrue(furnace.getOutput().is(ModItems.CAST_COPPER_BILLET.get()),"Copper fixture did not complete");
        double temperature=ItemHeat.celsius(furnace.getOutput(),h.getLevel());
        h.assertTrue(Math.abs(temperature-Math.min(1100,furnace.getTemperature()))<0.01,"Output used recipe threshold instead of actual clamped station heat");
        var before=furnace.getOutput().copy();var sim=furnace.getItemHandler(null).extractItem(3,64,true);
        h.assertTrue(ItemStack.matches(before,furnace.getOutput()) && ItemHeat.celsius(sim,h.getLevel())==temperature,"Simulated extraction changed or reheated output");
        var real=furnace.getItemHandler(null).extractItem(3,64,false);
        h.assertTrue(ItemHeat.celsius(real,h.getLevel())==temperature && furnace.getOutput().isEmpty(),"Real extraction lost absolute heat");h.succeed();
    }
}
