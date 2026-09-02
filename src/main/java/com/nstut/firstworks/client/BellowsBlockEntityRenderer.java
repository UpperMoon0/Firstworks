package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.BellowsBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Smooth two-part bellows animation: the leather bag collapses while the top board follows the stroke. */
public final class BellowsBlockEntityRenderer implements BlockEntityRenderer<BellowsBlockEntity> {
    public static final ModelResourceLocation BAG = ModelResourceLocation.standalone(Firstworks.id("block/bellows_bag"));
    public static final ModelResourceLocation TOP = ModelResourceLocation.standalone(Firstworks.id("block/bellows_top"));

    public BellowsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(BellowsBlockEntity bellows, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        float compression = bellows.getCompression(partialTick);
        pose.pushPose();
        rotateToFacing(pose, bellows.getBlockState().getValue(BellowsBlock.FACING));

        pose.pushPose();
        pose.translate(0.0, 0.19, 0.0);
        pose.scale(1.0F, 1.0F - compression * 0.42F, 1.0F);
        pose.translate(0.0, -0.19, 0.0);
        renderPartial(bellows, BAG, pose, buffers, light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.0, -compression * 0.20, 0.0);
        renderPartial(bellows, TOP, pose, buffers, light);
        pose.popPose();

        pose.popPose();
    }

    private static void renderPartial(BellowsBlockEntity bellows, ModelResourceLocation modelLocation,
                                      PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(modelLocation);
        if (model == minecraft.getModelManager().getMissingModel()) return;
        BlockState state = bellows.getBlockState();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state, model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
    }

    private static void rotateToFacing(PoseStack pose, Direction facing) {
        float rotation = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5, 0.0, -0.5);
    }
}
