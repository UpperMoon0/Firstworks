package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class LoomInput {
    private static BlockPos held;
    private static net.minecraft.client.multiplayer.ClientLevel heldLevel;
    private static boolean updateGrip() {
        Minecraft mc = Minecraft.getInstance();
        if (heldLevel != mc.level) { held = null; heldLevel = mc.level; }
        boolean use = mc.player != null && mc.level != null && mc.screen == null && !mc.isPaused()
                && mc.options.keyUse.isDown() && mc.player.getMainHandItem().isEmpty() && !mc.player.isShiftKeyDown();
        if (held != null && (!use || !(mc.level.getBlockEntity(held) instanceof LoomBlockEntity loom)
                || LoomBlock.trackAim(mc.player, held, loom.getBlockState()) == null)) {
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new LoomInputPayload(held, false, false));
            held = null;
        }
        if (use && held == null && mc.hitResult instanceof BlockHitResult hit
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof LoomBlockEntity loom
                && loom.getActiveRecipe().isPresent() && !loom.isProcessCancelled()
                && LoomBlock.hitsShuttle(mc.player, loom)) held = hit.getBlockPos();
        return held != null;
    }

    /** Capture the initial grab before vanilla Use runs, then retain it through frame gaps. */
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && updateGrip()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (updateGrip()) PacketDistributor.sendToServer(new LoomInputPayload(held, true,
                com.nstut.firstworks.FirstworksClientConfig.LOOM_ASSISTANCE.get()));
    }
    private LoomInput() {}
}
