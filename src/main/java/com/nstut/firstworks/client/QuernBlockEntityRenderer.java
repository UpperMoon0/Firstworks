package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.quern.QuernBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class QuernBlockEntityRenderer implements BlockEntityRenderer<QuernBlockEntity> {
    public static final ModelResourceLocation RUNNER_MODEL = ModelResourceLocation.standalone(Firstworks.id("block/quern_runner"));

    public QuernBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(QuernBlockEntity q, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = q.getBlockState();
        BakedModel runnerModel = Minecraft.getInstance().getModelManager().getModel(RUNNER_MODEL);

        if (runnerModel != null && runnerModel != Minecraft.getInstance().getModelManager().getMissingModel()) {
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(q.getRotation(partial)));
            pose.translate(-0.5, 0.0, -0.5);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    pose.last(),
                    buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                    state,
                    runnerModel,
                    1.0F, 1.0F, 1.0F,
                    light,
                    overlay,
                    ModelData.EMPTY,
                    null
            );
            pose.popPose();
        }

        if (!q.getInput().isEmpty() || !q.getOutput().isEmpty()) {
            var stack = q.getOutput().isEmpty() ? q.getInput() : q.getOutput();
            pose.pushPose();
            pose.translate(0.5, 0.42, 0.5);
            pose.scale(0.35F, 0.35F, 0.35F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, q.getLevel(), 0);
            pose.popPose();
        }
    }
}
