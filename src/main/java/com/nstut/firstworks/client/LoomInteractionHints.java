package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Basic controls remain discoverable without an overlay integration installed. */
@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class LoomInteractionHints {
    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.level == null || mc.player == null
                || !(mc.hitResult instanceof BlockHitResult hit)
                || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof LoomBlockEntity loom)) return;
        var hint = mc.player.isShiftKeyDown()
                ? net.minecraft.network.chat.Component.translatable("hint.firstworks.loom.retrieve")
                : !loom.getOutput().isEmpty() && !(loom.getActiveRecipe().isPresent()
                        && !loom.isProcessCancelled() && LoomBlock.hitsShuttle(mc.player, loom))
                    ? net.minecraft.network.chat.Component.translatable("hint.firstworks.loom.collect")
                    : loom.interactionHint();
        var graphics = event.getGuiGraphics();
        graphics.drawCenteredString(mc.font, hint, graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 18, 0xFFE8DFCF);
    }

    private LoomInteractionHints() {}
}
