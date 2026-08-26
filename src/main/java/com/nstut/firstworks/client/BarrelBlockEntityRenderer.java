package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public final class BarrelBlockEntityRenderer implements BlockEntityRenderer<BarrelBlockEntity> {
    private final ItemRenderer itemRenderer;

    public BarrelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(BarrelBlockEntity barrel, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (barrel.getBlockState().getValue(BarrelBlock.SEALED)) return;

        FluidStack inputFluid = barrel.getInputTank().getFluid();
        FluidStack outputFluid = barrel.getOutputTank().getFluid();
        int total = inputFluid.getAmount() + outputFluid.getAmount();
        float surface = 2.05F / 16.0F;
        if (total > 0) {
            FluidStack visible = !outputFluid.isEmpty() ? outputFluid : inputFluid;
            surface = Mth.lerp((float) total / BarrelBlockEntity.CAPACITY,
                    2.05F / 16.0F, 13.6F / 16.0F);
            renderFluidSurface(barrel, visible, surface, poseStack, buffers, packedLight);
        }

        ItemStack input = barrel.getIngredient();
        ItemStack output = barrel.getOutput();
        if (!input.isEmpty() && !output.isEmpty()) {
            renderItem(barrel, input, 0.37F, Math.min(surface + 0.035F, 0.86F), 0.5F,
                    poseStack, buffers, packedLight, packedOverlay);
            renderItem(barrel, output, 0.63F, Math.min(surface + 0.035F, 0.86F), 0.5F,
                    poseStack, buffers, packedLight, packedOverlay);
        } else {
            ItemStack visible = output.isEmpty() ? input : output;
            if (!visible.isEmpty()) {
                renderItem(barrel, visible, 0.5F, Math.min(surface + 0.035F, 0.86F), 0.5F,
                        poseStack, buffers, packedLight, packedOverlay);
            }
        }
    }

    private static void renderFluidSurface(BarrelBlockEntity barrel, FluidStack fluid, float y,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = FluidSpriteCache.getSprite(properties.getStillTexture(fluid));
        int tint = properties.getTintColor(fluid);
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float red = ((tint >>> 16) & 0xFF) / 255.0F;
        float green = ((tint >>> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        if (alpha == 0.0F) alpha = 1.0F;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer vertices = buffers.getBuffer(Sheets.translucentItemSheet());
        float min = 3.05F / 16.0F;
        float max = 12.95F / 16.0F;
        int light = packedLight;
        vertex(vertices, matrix, pose, min, y, min, red, green, blue, alpha, sprite.getU0(), sprite.getV0(), light);
        vertex(vertices, matrix, pose, min, y, max, red, green, blue, alpha, sprite.getU0(), sprite.getV1(), light);
        vertex(vertices, matrix, pose, max, y, max, red, green, blue, alpha, sprite.getU1(), sprite.getV1(), light);
        vertex(vertices, matrix, pose, max, y, min, red, green, blue, alpha, sprite.getU1(), sprite.getV0(), light);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, PoseStack.Pose pose,
            float x, float y, float z, float red, float green, float blue, float alpha,
            float u, float v, int light) {
        vertices.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(0)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private void renderItem(BarrelBlockEntity barrel, ItemStack stack, float x, float y, float z,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.34F, 0.34F, 0.34F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, buffers, barrel.getLevel(), 0);
        poseStack.popPose();
    }
}
