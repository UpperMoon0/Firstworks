package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlock;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

public final class BrickMoldBlockEntityRenderer implements BlockEntityRenderer<BrickMoldBlockEntity> {
    private final ItemRenderer itemRenderer;

    public BrickMoldBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(BrickMoldBlockEntity mold, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack input = mold.getInput();
        ItemStack output = mold.getOutput();
        ItemStack visible = output.isEmpty() ? input : output;
        if (visible.isEmpty()) return;

        Direction facing = mold.getBlockState().getValue(BrickMoldBlock.FACING);
        float rotation = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };

        float animation = Math.max(0.0F, mold.getPressAnimationTicks() - partialTick)
                / BrickMoldBlockEntity.PRESS_ANIMATION_TICKS;
        if (!output.isEmpty()) {
            renderPressedClay(facing, animation, 1.0F, output,
                    poseStack, buffers, packedLight, packedOverlay);
            return;
        }
        var recipe = mold.getMatchingRecipe();
        int requiredPresses = recipe.map(holder -> Math.max(1, holder.value().presses())).orElse(1);
        float compression = Mth.clamp((float) mold.getPressProgress() / requiredPresses, 0.0F, 1.0F);
        if (compression < 1.0F) {
            float itemScale = 0.42F * (1.0F - 0.65F * compression);
            float itemY = Mth.lerp(compression, 0.20F, 0.16F);
            renderSlotItem(mold, visible, 0.50F, itemY, 0.50F, rotation, itemScale,
                    poseStack, buffers, packedLight, packedOverlay);
        }
        if (compression > 0.0F && recipe.isPresent()) {
            renderPressedClay(facing, animation, compression, recipe.get().value().result(),
                    poseStack, buffers, packedLight, packedOverlay);
        }
    }

    private void renderSlotItem(BrickMoldBlockEntity mold, ItemStack stack, float x, float y, float z,
            float rotation, float scale, PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, buffers, mold.getLevel(), 0);
        poseStack.popPose();
    }

    private void renderPressedClay(Direction facing, float animation, float compression, ItemStack output,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        TextureAtlasSprite clay = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(Blocks.CLAY.defaultBlockState()).getParticleIcon();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        float rotation = switch (facing) {
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        float halfWidth = Mth.lerp(compression, 1.8F / 16.0F, 4.95F / 16.0F);
        float halfDepth = Mth.lerp(compression, 1.4F / 16.0F, 2.95F / 16.0F);
        float minX = 0.5F - halfWidth;
        float maxX = 0.5F + halfWidth;
        float minZ = 0.5F - halfDepth;
        float maxZ = 0.5F + halfDepth;
        float y = Mth.lerp(compression, 3.25F / 16.0F, 2.9F / 16.0F)
                + animation * animation * 0.04F;
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer vertices = buffers.getBuffer(Sheets.solidBlockSheet());
        int tint = outputTint(output);
        pressedClayVertex(vertices, matrix, pose, minX, y, minZ,
                clay.getU0(), clay.getV0(), tint, packedLight, packedOverlay);
        pressedClayVertex(vertices, matrix, pose, minX, y, maxZ,
                clay.getU0(), clay.getV1(), tint, packedLight, packedOverlay);
        pressedClayVertex(vertices, matrix, pose, maxX, y, maxZ,
                clay.getU1(), clay.getV1(), tint, packedLight, packedOverlay);
        pressedClayVertex(vertices, matrix, pose, maxX, y, minZ,
                clay.getU1(), clay.getV0(), tint, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private int outputTint(ItemStack output) {
        int registeredTint = Minecraft.getInstance().getItemColors().getColor(output, 0);
        if (registeredTint != -1) return registeredTint | 0xFF000000;

        TextureAtlasSprite sprite = itemRenderer.getModel(output, null, null, 0).getParticleIcon();
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        long red = 0;
        long green = 0;
        long blue = 0;
        long weight = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = sprite.getPixelRGBA(0, x, y);
                int alpha = pixel >>> 24;
                if (alpha < 16) continue;
                red += (long) (pixel & 0xFF) * alpha;
                green += (long) ((pixel >>> 8) & 0xFF) * alpha;
                blue += (long) ((pixel >>> 16) & 0xFF) * alpha;
                weight += alpha;
            }
        }
        if (weight == 0) return 0xFFFFFFFF;
        int averageRed = (int) (red / weight);
        int averageGreen = (int) (green / weight);
        int averageBlue = (int) (blue / weight);
        int brightest = Math.max(averageRed, Math.max(averageGreen, averageBlue));
        if (brightest > 0) {
            averageRed = averageRed * 255 / brightest;
            averageGreen = averageGreen * 255 / brightest;
            averageBlue = averageBlue * 255 / brightest;
        }
        return 0xFF000000 | averageRed << 16 | averageGreen << 8 | averageBlue;
    }

    private static void pressedClayVertex(VertexConsumer vertices, Matrix4f matrix, PoseStack.Pose pose,
            float x, float y, float z, float u, float v, int tint, int light, int overlay) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(tint)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
