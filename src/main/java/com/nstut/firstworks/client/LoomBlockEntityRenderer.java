package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class LoomBlockEntityRenderer implements BlockEntityRenderer<LoomBlockEntity> {
    private final ItemRenderer itemRenderer;

    public LoomBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(LoomBlockEntity loom, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                loom.getBlockState().getValue(LoomBlock.FACING).toYRot() + 180.0F));
        poseStack.translate(-0.5, 0, -0.5);

        ItemStack visibleOutput = loom.getOutput();
        if (visibleOutput.isEmpty()) {
            visibleOutput = loom.getMatchingRecipe().map(holder -> holder.value().result()).orElse(ItemStack.EMPTY);
        }
        if (!visibleOutput.isEmpty()) {
            renderWovenThreads(loom, visibleOutput, poseStack, buffers, packedLight, packedOverlay);
        }
        renderShuttle(loom, partialTick, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderWovenThreads(LoomBlockEntity loom, ItemStack output, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        int required = loom.getMatchingRecipe().map(holder -> Math.max(1, holder.value().strokes())).orElse(1);
        float fraction = loom.getOutput().isEmpty() ? (float) loom.getProgress() / required : 1.0F;
        if (fraction <= 0.0F) return;

        TextureAtlasSprite sprite = itemRenderer.getModel(output, loom.getLevel(), null, 0).getParticleIcon();
        int tint = Minecraft.getInstance().getItemColors().getColor(output, 0);
        if (tint == -1) tint = 0xFFFFFFFF;
        else tint |= 0xFF000000;
        VertexConsumer vertices = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        float minX = 3.6F / 16.0F;
        float maxX = 12.4F / 16.0F;
        float minY = 7.0F / 16.0F;
        float maxY = minY + fraction * (4.0F / 16.0F);
        float z = 7.45F / 16.0F;
        float maxV = Mth.lerp(fraction, sprite.getV1(), sprite.getV0());
        vertex(vertices, matrix, minX, minY, z, sprite.getU0(), sprite.getV1(), tint, packedLight, packedOverlay);
        vertex(vertices, matrix, maxX, minY, z, sprite.getU1(), sprite.getV1(), tint, packedLight, packedOverlay);
        vertex(vertices, matrix, maxX, maxY, z, sprite.getU1(), maxV, tint, packedLight, packedOverlay);
        vertex(vertices, matrix, minX, maxY, z, sprite.getU0(), maxV, tint, packedLight, packedOverlay);
        float backZ = z + 0.005F;
        vertex(vertices, matrix, minX, maxY, backZ, sprite.getU0(), maxV, tint, packedLight, packedOverlay);
        vertex(vertices, matrix, maxX, maxY, backZ, sprite.getU1(), maxV, tint, packedLight, packedOverlay);
        vertex(vertices, matrix, maxX, minY, backZ, sprite.getU1(), sprite.getV1(), tint, packedLight, packedOverlay);
        vertex(vertices, matrix, minX, minY, backZ, sprite.getU0(), sprite.getV1(), tint, packedLight, packedOverlay);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
            float u, float v, int color, int light, int overlay) {
        vertices.addVertex(matrix, x, y, z).setColor(color).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
    }

    private void renderShuttle(LoomBlockEntity loom, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (loom.getInput().isEmpty() && loom.getOutput().isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F + loom.getShuttleOffset(partialTick), 8.35F / 16.0F, 5.5F / 16.0F);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.48F, 0.48F, 0.48F);
        itemRenderer.renderStatic(new ItemStack(net.minecraft.world.item.Items.STICK), ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, buffers, loom.getLevel(), 0);
        poseStack.popPose();
    }
}
