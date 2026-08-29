package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nstut.firstworks.content.mortar.MortarBlock;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

public final class MortarBlockEntityRenderer implements BlockEntityRenderer<MortarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public MortarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(MortarBlockEntity mortar, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = mortar.getBlockState().getValue(MortarBlock.FACING);
        float facingRotation = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };

        ItemStack visible = mortar.getOutput().isEmpty() ? mortar.getInput() : mortar.getOutput();
        if (!visible.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.24F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(facingRotation));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(0.28F, 0.28F, 0.28F);
            itemRenderer.renderStatic(visible, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, buffers, mortar.getLevel(), 0);
            poseStack.popPose();
        }

        float phase = mortar.getGrindingProgress(partialTick);
        float stroke = mortar.isGrinding() ? (float) Math.sin(phase * 0.72F) : 0.0F;
        TextureAtlasSprite stone = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(Blocks.STONE.defaultBlockState()).getParticleIcon();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.54F + Math.abs(stroke) * 0.025F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(facingRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(27.0F + stroke * 13.0F));
        renderCuboid(poseStack, buffers.getBuffer(Sheets.solidBlockSheet()), stone,
                -0.065F, -0.30F, -0.065F, 0.065F, 0.30F, 0.065F,
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCuboid(PoseStack poseStack, VertexConsumer vertices, TextureAtlasSprite sprite,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            int light, int overlay) {
        face(poseStack, vertices, sprite, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                0, -1, 0, light, overlay);
        face(poseStack, vertices, sprite, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
                0, 1, 0, light, overlay);
        face(poseStack, vertices, sprite, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                0, 0, -1, light, overlay);
        face(poseStack, vertices, sprite, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                0, 0, 1, light, overlay);
        face(poseStack, vertices, sprite, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                -1, 0, 0, light, overlay);
        face(poseStack, vertices, sprite, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                1, 0, 0, light, overlay);
    }

    private static void face(PoseStack poseStack, VertexConsumer vertices, TextureAtlasSprite sprite,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            float nx, float ny, float nz, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        vertex(vertices, matrix, pose, x1, y1, z1, sprite.getU0(), sprite.getV1(), nx, ny, nz, light, overlay);
        vertex(vertices, matrix, pose, x2, y2, z2, sprite.getU1(), sprite.getV1(), nx, ny, nz, light, overlay);
        vertex(vertices, matrix, pose, x3, y3, z3, sprite.getU1(), sprite.getV0(), nx, ny, nz, light, overlay);
        vertex(vertices, matrix, pose, x4, y4, z4, sprite.getU0(), sprite.getV0(), nx, ny, nz, light, overlay);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, float nx, float ny, float nz,
            int light, int overlay) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
