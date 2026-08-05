package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nstut.firstworks.content.TreeBarkItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

public final class TreeBarkItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final TreeBarkItemRenderer INSTANCE = new TreeBarkItemRenderer();

    public TreeBarkItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        String woodType = TreeBarkItem.woodType(stack);
        ResourceLocation textureLoc = TreeBarkTextureManager.getTextureLocation(woodType);
        List<TreeBarkTextureManager.QuadVertex> mesh = TreeBarkTextureManager.getMesh(woodType);

        if (mesh.isEmpty()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.itemEntityTranslucentCull(textureLoc));
        poseStack.pushPose();
        org.joml.Matrix4f matrix = poseStack.last().pose();
        org.joml.Matrix3f normalMatrix = poseStack.last().normal();

        int light = (displayContext == ItemDisplayContext.GUI) ? 0xF000F0 : combinedLight;

        org.joml.Vector3f norm = new org.joml.Vector3f();
        for (TreeBarkTextureManager.QuadVertex v : mesh) {
            norm.set(v.nx(), v.ny(), v.nz()).mul(normalMatrix);
            if (norm.lengthSquared() > 0) norm.normalize();

            consumer.addVertex(matrix, v.x(), v.y(), v.z())
                    .setColor(255, 255, 255, 255)
                    .setUv(v.u(), v.v())
                    .setOverlay(combinedOverlay)
                    .setLight(light)
                    .setNormal(norm.x(), norm.y(), norm.z());
        }

        poseStack.popPose();
    }
}
