package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopBlock;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * In-world visual language for the Stone/Copper workshop. Every process is readable from the block:
 * wheel motion and shaped clay, anvil deformation, kiln fire, and bellows-fed molten copper.
 */
public final class WorkshopBlockEntityRenderer implements BlockEntityRenderer<WorkshopBlockEntity> {
    public static final ModelResourceLocation POTTERY_HEAD = ModelResourceLocation.standalone(Firstworks.id("block/pottery_wheel_head"));
    public static final ModelResourceLocation KILN_EMBERS = ModelResourceLocation.standalone(Firstworks.id("block/kiln_embers"));
    public static final ModelResourceLocation CRUCIBLE_CONTENTS = ModelResourceLocation.standalone(Firstworks.id("block/crucible_furnace_contents"));

    public WorkshopBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WorkshopBlockEntity workshop, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = workshop.getBlockState();
        Direction facing = state.getValue(WorkshopBlock.FACING);
        pose.pushPose();
        rotateToFacing(pose, facing);
        switch (workshop.station()) {
            case WorkshopRecipe.POTTERY_WHEEL -> renderPotteryWheel(workshop, partialTick, pose, buffers, packedLight);
            case WorkshopRecipe.STONE_ANVIL -> renderStoneAnvil(workshop, partialTick, pose, buffers, packedLight);
            case WorkshopRecipe.KILN -> renderKiln(workshop, pose, buffers, packedLight);
            case WorkshopRecipe.CRUCIBLE_FURNACE -> renderCrucibleFurnace(workshop, pose, buffers, packedLight);
            default -> { }
        }
        pose.popPose();
    }

    private void renderPotteryWheel(WorkshopBlockEntity workshop, float partialTick, PoseStack pose,
                                    MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(workshop.getWheelRotation(partialTick)));
        pose.translate(-0.5, 0.0, -0.5);
        renderPartial(workshop, POTTERY_HEAD, pose, buffers, light);
        pose.popPose();

        ItemStack visible = workshop.getOutput();
        if (visible.isEmpty()) {
            visible = workshop.activeRecipe().map(holder -> holder.value().result()).orElse(workshop.getInput());
        }
        if (visible.isEmpty()) return;

        float progress = workshop.getProgressFraction();
        float shape = workshop.getOutput().isEmpty() ? Math.max(0.15F, progress) : 1.0F;
        pose.pushPose();
        pose.translate(0.5, 0.56 + shape * 0.035, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(workshop.getWheelRotation(partialTick)));
        float horizontal = 0.25F + shape * 0.17F;
        float vertical = 0.18F + shape * 0.28F;
        pose.scale(horizontal, vertical, horizontal);
        Minecraft.getInstance().getItemRenderer().renderStatic(visible, ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
        pose.popPose();
    }

    private void renderStoneAnvil(WorkshopBlockEntity workshop, float partialTick, PoseStack pose,
                                  MultiBufferSource buffers, int light) {
        ItemStack visible = workshop.getOutput().isEmpty() ? workshop.getInput() : workshop.getOutput();
        if (visible.isEmpty()) return;
        float fraction = workshop.getProgressFraction();
        float impact = workshop.getActionPulse(partialTick);
        pose.pushPose();
        pose.translate(0.5, 0.71 + impact * 0.025, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.scale(0.40F + fraction * 0.13F, 0.40F + fraction * 0.13F, 0.24F - fraction * 0.055F - impact * 0.025F);
        Minecraft.getInstance().getItemRenderer().renderStatic(visible, ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
        pose.popPose();
    }

    private void renderKiln(WorkshopBlockEntity workshop, PoseStack pose, MultiBufferSource buffers, int light) {
        if (workshop.isRunning()) renderPartial(workshop, KILN_EMBERS, pose, buffers, LightTexture.FULL_BRIGHT);
        ItemStack visible = workshop.getOutput().isEmpty() ? workshop.getInput() : workshop.getOutput();
        if (visible.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 0.34, 0.265);
        pose.mulPose(Axis.XP.rotationDegrees(18.0F));
        pose.scale(0.31F, 0.31F, 0.31F);
        Minecraft.getInstance().getItemRenderer().renderStatic(visible, ItemDisplayContext.FIXED,
                workshop.isRunning() ? LightTexture.FULL_BRIGHT : light,
                OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
        pose.popPose();
    }

    private void renderCrucibleFurnace(WorkshopBlockEntity workshop, PoseStack pose,
                                       MultiBufferSource buffers, int light) {
        if (workshop.isRunning() || workshop.getStokeTicks() > 0 || workshop.getProgress() > 0) {
            renderPartial(workshop, CRUCIBLE_CONTENTS, pose, buffers, LightTexture.FULL_BRIGHT);
        }

        if (!workshop.getCatalyst().isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.79, 0.72);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.scale(0.31F, 0.31F, 0.31F);
            Minecraft.getInstance().getItemRenderer().renderStatic(workshop.getCatalyst(), ItemDisplayContext.FIXED,
                    light, OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
            pose.popPose();
        }

        ItemStack metal = workshop.getOutput().isEmpty() ? workshop.getInput() : workshop.getOutput();
        if (!metal.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, workshop.getOutput().isEmpty() ? 0.69 : 0.84, workshop.getOutput().isEmpty() ? 0.5 : 0.72);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.scale(0.23F, 0.23F, 0.23F);
            Minecraft.getInstance().getItemRenderer().renderStatic(metal, ItemDisplayContext.FIXED,
                    workshop.isRunning() ? LightTexture.FULL_BRIGHT : light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
            pose.popPose();
        }
    }

    private static void renderPartial(WorkshopBlockEntity workshop, ModelResourceLocation modelLocation,
                                      PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(modelLocation);
        if (model == minecraft.getModelManager().getMissingModel()) return;
        BlockState state = workshop.getBlockState();
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
