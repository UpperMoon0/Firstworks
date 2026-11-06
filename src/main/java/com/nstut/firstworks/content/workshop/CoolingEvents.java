package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=Firstworks.MOD_ID)
public final class CoolingEvents {
    private static final java.util.Map<Player,BlockPos> QUENCHING = new java.util.WeakHashMap<>();
    public static boolean isQuenching(net.minecraft.world.entity.LivingEntity entity) { return entity instanceof Player p && QUENCHING.containsKey(p); }
    private static void endQuench(Player p,net.minecraft.world.InteractionHand hand) {
        if (!p.level().isClientSide) p.setItemInHand(hand,CoolingSystem.observe(p.getItemInHand(hand),p.level(),p.blockPosition(),CoolingProfiles.air()));
        QUENCHING.remove(p);
        p.stopUsingItem();
    }

    public static boolean filledCauldron(Level level,BlockPos pos) { return level.getBlockState(pos).is(Blocks.WATER_CAULDRON); }
    private static CoolingProfiles.Selected water() { return CoolingProfiles.select(Fluids.WATER.defaultFluidState()); }
    @SubscribeEvent public static void itemTick(EntityTickEvent.Post e) {
        if (!(e.getEntity() instanceof ItemEntity item) || item.level().isClientSide || !item.getItem().has(ModDataComponents.HEAT.get())) return;
        var pos=item.blockPosition();
        var fluid=item.level().getFluidState(pos);
        var medium=CoolingProfiles.air();
        if (!fluid.isEmpty() && item.getY()<pos.getY()+fluid.getHeight(item.level(),pos)) medium=CoolingProfiles.select(fluid);
        else if (filledCauldron(item.level(),pos)) {
            int fill=item.level().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL);
            double surface=pos.getY()+(6+3*fill)/16.0;
            if (item.getY()<surface && item.getBoundingBox().maxY>pos.getY()+0.25
                    && item.getX()>pos.getX()+0.125 && item.getX()<pos.getX()+0.875
                    && item.getZ()>pos.getZ()+0.125 && item.getZ()<pos.getZ()+0.875) medium=water();
        }
        item.setItem(CoolingSystem.observe(item.getItem(),item.level(),pos,medium));
    }
    @SubscribeEvent public static void playerTick(PlayerTickEvent.Post e) {
        var p=e.getEntity();
        if (isQuenching(p) && !p.isUsingItem()) QUENCHING.remove(p);
        if (p.level().isClientSide || p.tickCount%20!=0) return;
        for(int i=0;i<p.getInventory().getContainerSize();i++) {
            var stack=p.getInventory().getItem(i);
            if (isQuenching(p) && p.isUsingItem() && stack==p.getUseItem()) continue;
            if (stack.has(ModDataComponents.HEAT.get())) p.getInventory().setItem(i,CoolingSystem.observe(stack,p.level(),p.blockPosition(),CoolingProfiles.air()));
        }
    }
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Pre e) {
        if (e.getPlayer().level().isClientSide) return;
        if (!HeatProtection.allow(e.getPlayer(),e.getItemEntity().getItem(),net.minecraft.world.InteractionHand.MAIN_HAND)) e.setCanPickup(TriState.FALSE);
    }
    @SubscribeEvent public static void startQuench(PlayerInteractEvent.RightClickBlock e) {
        if (e.isCanceled() || !filledCauldron(e.getLevel(),e.getPos()) || !e.getItemStack().has(ModDataComponents.HEAT.get())) return;
        if (ItemHeat.celsius(e.getItemStack(),e.getLevel())<=20) return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.sidedSuccess(e.getLevel().isClientSide));
        if (isQuenching(e.getEntity()) && e.getEntity().isUsingItem()) return;
        QUENCHING.put(e.getEntity(),e.getPos().immutable());
        e.getEntity().startUsingItem(e.getHand());
    }
    @SubscribeEvent public static void useTick(LivingEntityUseItemEvent.Tick e) {
        if (!(e.getEntity() instanceof Player p) || !isQuenching(p)) return;
        if (!e.getItem().has(ModDataComponents.HEAT.get())) { endQuench(p,e.getHand()); return; }
        HitResult hit=p.pick(p.blockInteractionRange(),1,false);
        if (!(hit instanceof BlockHitResult block) || hit.getType()!=HitResult.Type.BLOCK || !filledCauldron(p.level(),block.getBlockPos()) || !block.getBlockPos().equals(QUENCHING.get(p))) {
            endQuench(p,e.getHand());return;
        }
        if (p.level().isClientSide) return;
        var result=CoolingSystem.observe(e.getItem(),p.level(),block.getBlockPos(),water());
        if (result!=e.getItem()) { p.setItemInHand(e.getHand(),result);endQuench(p,e.getHand()); }
        else if (!result.has(ModDataComponents.HEAT.get())) endQuench(p,e.getHand());
    }
    @SubscribeEvent public static void stopQuench(LivingEntityUseItemEvent.Stop e) {
        if (e.getEntity() instanceof Player p && isQuenching(p)) endQuench(p,e.getHand());
    }
    @SubscribeEvent public static void departing(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        var level = e.getEntity().level();
        if (e.getEntity() instanceof Player p) {
            if (isQuenching(p)) endQuench(p,p.getUsedItemHand());
            for (int i=0;i<p.getInventory().getContainerSize();i++) ItemHeat.checkpoint(p.getInventory().getItem(i),level);
        } else if (e.getEntity() instanceof ItemEntity item) ItemHeat.checkpoint(item.getItem(),level);
    }
    private CoolingEvents() {}
}
