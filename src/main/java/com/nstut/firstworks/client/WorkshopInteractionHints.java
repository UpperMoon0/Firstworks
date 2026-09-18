package com.nstut.firstworks.client;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.mortar.MortarBlock;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import com.nstut.firstworks.content.workshop.StoneAnvilBlock;
import com.nstut.firstworks.content.workshop.WorkshopBlock;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = Firstworks.MOD_ID, value = Dist.CLIENT)
public final class WorkshopInteractionHints {
    @SubscribeEvent public static void hint(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.player == null || mc.level == null
                || !(mc.hitResult instanceof BlockHitResult hit)) return;
        var entity = mc.level.getBlockEntity(hit.getBlockPos());
        Component text;
        if (entity instanceof MortarBlockEntity mortar && mc.player.getMainHandItem().isEmpty()) {
            text = mc.player.isShiftKeyDown() ? Component.translatable("hint.firstworks.retrieve")
                    : mortar.hint(MortarBlock.actionAt(hit.getBlockPos(), hit));
        } else if (entity instanceof WorkshopBlockEntity anvil && anvil.station().equals(WorkshopRecipe.STONE_ANVIL)) {
            boolean hammer = mc.player.getMainHandItem().is(ModTags.HAMMERS) || mc.player.getOffhandItem().is(ModTags.HAMMERS);
            text = !hammer && mc.player.isShiftKeyDown() ? Component.translatable("hint.firstworks.retrieve")
                    : anvil.anvilHint(StoneAnvilBlock.actionAt(anvil.getBlockState(), hit.getBlockPos(), hit), mc.player.isShiftKeyDown(), hammer);
        } else return;
        var graphics = event.getGuiGraphics();
        graphics.drawCenteredString(mc.font, text, graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 18, 0xFFE8DFCF);
    }

    @SubscribeEvent public static void highlight(RenderHighlightEvent.Block event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().is(ModTags.HAMMERS)
                || mc.player.getOffhandItem().is(ModTags.HAMMERS))) return;
        var hit = event.getTarget();
        var state = mc.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof StoneAnvilBlock)) return;
        String action = StoneAnvilBlock.actionAt(state, hit.getBlockPos(), hit);
        var shape = WorkshopBlock.makeHorizontalShapes(StoneAnvilBlock.zone(action)).get(state.getValue(WorkshopBlock.FACING));
        if (shape.isEmpty()) return;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(hit.getBlockPos().getX() - camera.x, hit.getBlockPos().getY() - camera.y, hit.getBlockPos().getZ() - camera.z);
        var vertices = event.getMultiBufferSource().getBuffer(RenderType.lines());
        shape.forAllBoxes((x0, y0, z0, x1, y1, z1) ->
                LevelRenderer.renderLineBox(pose, vertices, x0, y0, z0, x1, y1, z1, 0.9F, 0.72F, 0.45F, 0.6F));
        pose.popPose();
        event.setCanceled(true);
    }
    private WorkshopInteractionHints() {}
}
