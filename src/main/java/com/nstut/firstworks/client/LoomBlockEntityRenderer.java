package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

public final class LoomBlockEntityRenderer implements BlockEntityRenderer<LoomBlockEntity> {
    public static final ModelResourceLocation COPPER_BEATER_MODEL = ModelResourceLocation.standalone(Firstworks.id("block/copper_loom_beater"));
    private final ItemRenderer itemRenderer;

    public LoomBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void render(LoomBlockEntity loom, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                loom.getBlockState().getValue(LoomBlock.FACING).toYRot() + 180.0F));
        poseStack.translate(-0.5, 0, -0.5);

        ItemStack visibleOutput = loom.getOutput();
        if (visibleOutput.isEmpty()) {
            visibleOutput = loom.getMatchingRecipe().map(holder -> holder.value().result()).orElse(ItemStack.EMPTY);
        }
        if (!visibleOutput.isEmpty()) {
            renderWarpThreads(loom, visibleOutput, poseStack, buffers, packedLight, packedOverlay);
            renderWovenThreads(loom, visibleOutput, poseStack, buffers, packedLight, packedOverlay);
        }
        renderShuttle(loom, visibleOutput, partialTick, poseStack, buffers, packedLight, packedOverlay);
        if (loom.getBlockState().is(ModBlocks.COPPER_LOOM.get())) {
            renderCopperBeater(loom, partialTick, poseStack, buffers, packedLight);
        }
        poseStack.popPose();
    }

    private void renderWarpThreads(LoomBlockEntity loom, ItemStack output, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        TextureAtlasSprite sprite = itemRenderer.getModel(output, null, null, 0).getParticleIcon();
        int tint = outputTint(output);
        VertexConsumer vertices = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        float minY = 6.75F / 16.0F;
        float maxY = 11.5F / 16.0F;
        float z = 7.5F / 16.0F;
        int totalStrands = 12;
        int requiredInput = loom.getMatchingRecipe()
                .map(holder -> Math.max(1, holder.value().inputCount())).orElse(1);
        float loadedFraction = loom.getOutput().isEmpty()
                ? Mth.clamp((float) loom.getInput().getCount() / requiredInput, 0.0F, 1.0F)
                : 1.0F;
        int visibleStrands = Mth.ceil(totalStrands * loadedFraction);
        float centerV = (sprite.getV0() + sprite.getV1()) * 0.5F;
        float dv = (sprite.getV1() - sprite.getV0()) / 64.0F;
        for (int i = 0; i < visibleStrands; i++) {
            float centerX = Mth.lerp((i + 0.5F) / totalStrands, 3.75F / 16.0F, 12.25F / 16.0F);
            float halfWidth = 0.11F / 16.0F;
            float u = (sprite.getU0() + sprite.getU1()) * 0.5F;
            float du = (sprite.getU1() - sprite.getU0()) / 64.0F;
            quadBothSides(vertices, matrix, centerX - halfWidth, minY, centerX + halfWidth, maxY, z,
                    u - du, centerV + dv, u + du, centerV - dv,
                    tint, packedLight, packedOverlay);
        }
    }

    private void renderWovenThreads(LoomBlockEntity loom, ItemStack output, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        int required = loom.getRequiredStrokes();
        float fraction = loom.getOutput().isEmpty() ? (float) loom.getProgress() / required : 1.0F;
        if (fraction <= 0.0F) return;

        TextureAtlasSprite sprite = itemRenderer.getModel(output, loom.getLevel(), null, 0).getParticleIcon();
        int tint = outputTint(output);
        VertexConsumer vertices = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        float minX = 3.75F / 16.0F;
        float maxX = 12.25F / 16.0F;
        float minY = 6.75F / 16.0F;
        float fullHeight = 4.75F / 16.0F;
        float maxY = minY + fraction * fullHeight;
        float z = 7.35F / 16.0F;
        float physicalAspect = fullHeight / (maxX - minX);
        float croppedVSpan = (sprite.getV1() - sprite.getV0()) * physicalAspect;
        float centerV = (sprite.getV0() + sprite.getV1()) * 0.5F;
        float bottomV = centerV + croppedVSpan * 0.5F;
        float topV = Mth.lerp(fraction, bottomV, centerV - croppedVSpan * 0.5F);
        quadBothSides(vertices, matrix, minX, minY, maxX, maxY, z,
                sprite.getU0(), bottomV, sprite.getU1(), topV,
                tint, packedLight, packedOverlay);
    }

    private static int outputTint(ItemStack output) {
        int tint = Minecraft.getInstance().getItemColors().getColor(output, 0);
        return tint == -1 ? 0xFFFFFFFF : tint | 0xFF000000;
    }

    private static void quadBothSides(VertexConsumer vertices, Matrix4f matrix,
            float minX, float minY, float maxX, float maxY, float z,
            float minU, float minV, float maxU, float maxV,
            int color, int light, int overlay) {
        vertex(vertices, matrix, minX, minY, z, minU, minV, color, light, overlay, 0, 0, -1);
        vertex(vertices, matrix, minX, maxY, z, minU, maxV, color, light, overlay, 0, 0, -1);
        vertex(vertices, matrix, maxX, maxY, z, maxU, maxV, color, light, overlay, 0, 0, -1);
        vertex(vertices, matrix, maxX, minY, z, maxU, minV, color, light, overlay, 0, 0, -1);
        float backZ = z + 0.002F;
        vertex(vertices, matrix, maxX, minY, backZ, maxU, minV, color, light, overlay, 0, 0, 1);
        vertex(vertices, matrix, maxX, maxY, backZ, maxU, maxV, color, light, overlay, 0, 0, 1);
        vertex(vertices, matrix, minX, maxY, backZ, minU, maxV, color, light, overlay, 0, 0, 1);
        vertex(vertices, matrix, minX, minY, backZ, minU, minV, color, light, overlay, 0, 0, 1);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
            float u, float v, int color, int light, int overlay, float nx, float ny, float nz) {
        vertices.addVertex(matrix, x, y, z).setColor(color).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
    }

    private void renderShuttle(LoomBlockEntity loom, ItemStack output, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (loom.getInput().isEmpty() && loom.getOutput().isEmpty()) return;
        TextureAtlasSprite wood = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(loom.getBlockState()).getParticleIcon();
        TextureAtlasSprite thread = output.isEmpty() ? wood
                : itemRenderer.getModel(output, loom.getLevel(), null, 0).getParticleIcon();
        int threadTint = output.isEmpty() ? 0xFFFFFFFF : outputTint(output);
        VertexConsumer vertices = buffers.getBuffer(Sheets.cutoutBlockSheet());
        Matrix4f matrix = poseStack.last().pose();
        float x = 0.5F + loom.getShuttleOffset(partialTick);
        float y = 9.0F / 16.0F;
        float z = 5.35F / 16.0F;
        renderBox(vertices, matrix, x - 3.0F / 16.0F, y - 0.65F / 16.0F, z - 0.65F / 16.0F,
                x + 3.0F / 16.0F, y + 0.65F / 16.0F, z + 0.65F / 16.0F,
                wood, 0xFFFFFFFF, packedLight, packedOverlay);
        renderBox(vertices, matrix, x - 3.5F / 16.0F, y - 0.3F / 16.0F, z - 0.3F / 16.0F,
                x - 2.85F / 16.0F, y + 0.3F / 16.0F, z + 0.3F / 16.0F,
                wood, 0xFFFFFFFF, packedLight, packedOverlay);
        renderBox(vertices, matrix, x + 2.85F / 16.0F, y - 0.3F / 16.0F, z - 0.3F / 16.0F,
                x + 3.5F / 16.0F, y + 0.3F / 16.0F, z + 0.3F / 16.0F,
                wood, 0xFFFFFFFF, packedLight, packedOverlay);
        renderBox(vertices, matrix, x - 0.8F / 16.0F, y - 0.9F / 16.0F, z - 0.9F / 16.0F,
                x - 0.38F / 16.0F, y + 0.9F / 16.0F, z + 0.9F / 16.0F,
                thread, threadTint, packedLight, packedOverlay);
        renderBox(vertices, matrix, x - 0.22F / 16.0F, y - 0.9F / 16.0F, z - 0.9F / 16.0F,
                x + 0.22F / 16.0F, y + 0.9F / 16.0F, z + 0.9F / 16.0F,
                thread, threadTint, packedLight, packedOverlay);
        renderBox(vertices, matrix, x + 0.38F / 16.0F, y - 0.9F / 16.0F, z - 0.9F / 16.0F,
                x + 0.8F / 16.0F, y + 0.9F / 16.0F, z + 0.9F / 16.0F,
                thread, threadTint, packedLight, packedOverlay);
    }

    private static void renderBox(VertexConsumer vertices, Matrix4f matrix,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            TextureAtlasSprite sprite, int color, int light, int overlay) {
        float width = (maxX - minX) * 16.0F;
        float height = (maxY - minY) * 16.0F;
        float depth = (maxZ - minZ) * 16.0F;
        float widthU0 = sprite.getU(0.5F - width / 32.0F);
        float widthU1 = sprite.getU(0.5F + width / 32.0F);
        float depthU0 = sprite.getU(0.5F - depth / 32.0F);
        float depthU1 = sprite.getU(0.5F + depth / 32.0F);
        float heightV0 = sprite.getV(0.5F - height / 32.0F);
        float heightV1 = sprite.getV(0.5F + height / 32.0F);
        float depthV0 = sprite.getV(0.5F - depth / 32.0F);
        float depthV1 = sprite.getV(0.5F + depth / 32.0F);
        face(vertices, matrix, minX,minY,minZ, minX,maxY,minZ, maxX,maxY,minZ, maxX,minY,minZ,
                widthU0,heightV1,widthU1,heightV0,color,light,overlay,0,0,-1);
        face(vertices, matrix, maxX,minY,maxZ, maxX,maxY,maxZ, minX,maxY,maxZ, minX,minY,maxZ,
                widthU0,heightV1,widthU1,heightV0,color,light,overlay,0,0,1);
        face(vertices, matrix, minX,minY,maxZ, minX,maxY,maxZ, minX,maxY,minZ, minX,minY,minZ,
                depthU0,heightV1,depthU1,heightV0,color,light,overlay,-1,0,0);
        face(vertices, matrix, maxX,minY,minZ, maxX,maxY,minZ, maxX,maxY,maxZ, maxX,minY,maxZ,
                depthU0,heightV1,depthU1,heightV0,color,light,overlay,1,0,0);
        face(vertices, matrix, minX,maxY,minZ, minX,maxY,maxZ, maxX,maxY,maxZ, maxX,maxY,minZ,
                widthU0,depthV1,widthU1,depthV0,color,light,overlay,0,1,0);
        face(vertices, matrix, minX,minY,maxZ, minX,minY,minZ, maxX,minY,minZ, maxX,minY,maxZ,
                widthU0,depthV1,widthU1,depthV0,color,light,overlay,0,-1,0);
    }

    private static void face(VertexConsumer vertices, Matrix4f matrix,
            float x1,float y1,float z1, float x2,float y2,float z2,
            float x3,float y3,float z3, float x4,float y4,float z4,
            float u0,float v0,float u1,float v1, int color,int light,int overlay,
            float nx,float ny,float nz) {
        vertex(vertices,matrix,x1,y1,z1,u0,v0,color,light,overlay,nx,ny,nz);
        vertex(vertices,matrix,x2,y2,z2,u1,v0,color,light,overlay,nx,ny,nz);
        vertex(vertices,matrix,x3,y3,z3,u1,v1,color,light,overlay,nx,ny,nz);
        vertex(vertices,matrix,x4,y4,z4,u0,v1,color,light,overlay,nx,ny,nz);
    }

    private static void renderCopperBeater(LoomBlockEntity loom, float partialTick, PoseStack pose,
                                           MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(COPPER_BEATER_MODEL);
        if (model == minecraft.getModelManager().getMissingModel()) return;
        float stroke = loom.getStrokeAnimation(partialTick);
        pose.pushPose();
        pose.translate(0.5, 0.68, 0.46);
        pose.mulPose(Axis.XP.rotationDegrees(-17.0F * stroke));
        pose.translate(-0.5, -0.68, -0.46);
        BlockState state = loom.getBlockState();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state, model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        pose.popPose();
    }
}
