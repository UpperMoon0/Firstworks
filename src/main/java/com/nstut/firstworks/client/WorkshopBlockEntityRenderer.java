package com.nstut.firstworks.client;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.level.block.Blocks;
import com.nstut.firstworks.content.workshop.ForgeData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.registry.ModItems;
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
 * wheel motion and shaped clay, anvil deformation, bellows-fed molten copper.
 */
public final class WorkshopBlockEntityRenderer implements BlockEntityRenderer<WorkshopBlockEntity> {
    public static final ModelResourceLocation POTTERY_HEAD = ModelResourceLocation.standalone(Firstworks.id("block/pottery_wheel_head"));
    public static final ModelResourceLocation CRUCIBLE_CONTENTS = ModelResourceLocation.standalone(Firstworks.id("block/crucible_furnace_contents"));

    public static final ModelResourceLocation CASTING_MOLD = ModelResourceLocation.standalone(Firstworks.id("block/furnace_casting_mold"));
    public static final ModelResourceLocation CASTING_METAL = ModelResourceLocation.standalone(Firstworks.id("block/furnace_casting_metal"));

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
        var forge = workshop.activeRecipe().flatMap(h -> h.value().forge());
        if (workshop.getOutput().isEmpty() && forge.isPresent() && forge.get().visual().isPresent()) {
            renderForgeWorkpiece(workshop, forge.get(), pose, buffers, light);
            return;
        }
        float fraction = workshop.getProgressFraction();
        float impact = workshop.getActionPulse(partialTick);
        pose.pushPose();
        pose.translate(0.5, 0.71 + impact * 0.025, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        if (forge.isPresent()) pose.scale(0.40F, 0.40F, 0.40F);
        else pose.scale(0.40F + fraction * 0.13F, 0.40F + fraction * 0.13F, 0.24F - fraction * 0.055F - impact * 0.025F);
        Minecraft.getInstance().getItemRenderer().renderStatic(visible, ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
        pose.popPose();
    }

    private void renderForgeWorkpiece(WorkshopBlockEntity workshop, ForgeData data,
                                      PoseStack pose, MultiBufferSource buffers, int light) {
        var visual = data.visual().orElseThrow();
        int workLight = workshop.getForgeHeat() > 0 ? LightTexture.FULL_BRIGHT : light;
        if (visual.type().equals("stages") && !visual.models().isEmpty()) {
            var location = ModelResourceLocation.standalone(visual.models().get(Math.min(workshop.getProgress(), visual.models().size() - 1)));
            var models = Minecraft.getInstance().getModelManager();
            if (models.getModel(location) != models.getMissingModel()) {
                renderPartial(workshop, location, pose, buffers, workLight);
                return;
            }
        }
        float length = visual.length();
        float width = visual.width();
        float height = visual.initialProfile().equals("plate") ? visual.height() * 0.5F : visual.height();
        float bend = 0;
        for (String action : data.actions().subList(0, Math.min(workshop.getProgress(), data.actions().size()))) {
            switch (action) {
                case "flatten" -> { height *= 0.75F; width *= 1.2F; }
                case "draw" -> { length *= 1.2F; width *= 0.9F; }
                case "bend" -> bend = Math.min(60, bend + 25);
            }
        }
        length = Math.min(0.6F, length);
        width = Math.min(0.45F, width);
        height = Math.max(0.025F, height);
        var sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(
                Blocks.COPPER_BLOCK.defaultBlockState()).getParticleIcon();
        var vertices = buffers.getBuffer(Sheets.solidBlockSheet());
        pose.pushPose();
        pose.translate(0.5, 10.62 / 16.0, 0.5);
        MortarBlockEntityRenderer.renderCuboid(pose, vertices, sprite, -width / 2, 0, -length / 2, width / 2, height, 0,
                workLight, OverlayTexture.NO_OVERLAY);
        pose.mulPose(Axis.XP.rotationDegrees(-bend));
        MortarBlockEntityRenderer.renderCuboid(pose, vertices, sprite, -width / 2, 0, 0, width / 2, height, length / 2,
                workLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private void renderCrucibleFurnace(WorkshopBlockEntity workshop, PoseStack pose,
                                       MultiBufferSource buffers, int light) {
        if (workshop.isHot()) {
            renderPartial(workshop, CRUCIBLE_CONTENTS, pose, buffers, LightTexture.FULL_BRIGHT);
        }

        boolean castingMold = workshop.getCatalyst().is(ModItems.CASTING_MOLD.get());
        if (castingMold) {
            renderPartial(workshop, CASTING_MOLD, pose, buffers, light);
            if (workshop.getOutput().is(ModItems.CAST_COPPER_BILLET.get())
                    || workshop.isHot() && workshop.getProgressFraction() > 0.5F) {
                renderPartial(workshop, CASTING_METAL, pose, buffers,
                        workshop.isHot() ? LightTexture.FULL_BRIGHT : light);
            }
        } else if (!workshop.getCatalyst().isEmpty()) {
            renderFurnaceItem(workshop, workshop.getCatalyst(), 0.82, 0.82, pose, buffers, light);
        }

        if (!workshop.getOutput().isEmpty()) {
            if (!castingMold || !workshop.getOutput().is(ModItems.CAST_COPPER_BILLET.get())) {
                renderFurnaceItem(workshop, workshop.getOutput(), 0.85, 0.82, pose, buffers, light);
            }
        } else if (!workshop.getInput().isEmpty() && !workshop.isHot()) {
            renderFurnaceItem(workshop, workshop.getInput(), 0.66, 0.44, pose, buffers, light);
        }
    }

    private void renderFurnaceItem(WorkshopBlockEntity workshop, ItemStack stack, double y, double z,
                                   PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.5, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(0.28F, 0.28F, 0.28F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, workshop.getLevel(), 0);
        pose.popPose();
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
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> -270.0F;
            default -> 0.0F;
        };
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(-0.5, 0.0, -0.5);
    }
}
