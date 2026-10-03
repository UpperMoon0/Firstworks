package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nstut.firstworks.content.workshop.ItemHeat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import java.util.ArrayList;
import java.util.List;

/** Derives an emissive overlay from the item's own quads, preserving its texture and silhouette. */
public final class HeatGlowModel extends BakedModelWrapper<BakedModel> {
    private final float heat;
    private final boolean gui;
    public HeatGlowModel(BakedModel original) { this(original, 0); }
    private HeatGlowModel(BakedModel original, float heat) { this(original, heat, false); }
    private HeatGlowModel(BakedModel original, float heat, boolean gui) { super(original); this.heat = heat; this.gui = gui; }

    @Override public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
                BakedModel resolved = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
                if (resolved == null) return null;
                float fraction = ItemHeat.fraction(stack, level != null ? level : Minecraft.getInstance().level);
                return fraction > 0 && !resolved.isCustomRenderer() ? new HeatGlowModel(resolved, fraction) : resolved;
            }
        };
    }
    @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
        return new HeatGlowModel(originalModel.applyTransform(context, pose, left), heat, context == ItemDisplayContext.GUI);
    }
    @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        var passes = originalModel.getRenderPasses(stack, fabulous);
        if (heat <= 0) return passes;
        var result = new ArrayList<BakedModel>(passes);
        for (BakedModel pass : passes) result.add(new Overlay(pass, heat, gui));
        return result;
    }
    public static int color(float heat) {
        int alpha = Math.round(200 * heat);
        int green = Math.round(55 + 155 * heat);
        int blue = Math.round(12 + 85 * heat);
        return alpha << 24 | 255 << 16 | green << 8 | blue;
    }
    private static final class Overlay extends BakedModelWrapper<BakedModel> {
        private final float heat;
        private final boolean gui;
        Overlay(BakedModel original, float heat, boolean gui) { super(original); this.heat = heat; this.gui = gui; }
        @Override public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(gui ? HeatRenderType.GUI_GLOW : HeatRenderType.GLOW);
        }
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return originalModel.getQuads(state, side, random).stream().map(q -> {
                var quad = new BakedQuad(q.getVertices().clone(), -1, q.getDirection(), q.getSprite(), false, false);
                var vertices = quad.getVertices();
                var normal = q.getDirection();
                for (int v = 0; v < 4; v++) {
                    int p = v * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                    vertices[p] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[p]) + normal.getStepX() * 0.0005F);
                    vertices[p + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[p + 1]) + normal.getStepY() * 0.0005F);
                    vertices[p + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[p + 2]) + normal.getStepZ() * 0.0005F);
                }
                QuadTransformers.applyingColor(color(heat)).processInPlace(quad);
                QuadTransformers.settingMaxEmissivity().processInPlace(quad);
                return quad;
            }).toList();
        }
    }
}
