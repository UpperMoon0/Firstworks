package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

/** Used by the deforming anvil mesh and custom recipe stage models. */
final class HeatOverlay extends VertexConsumerWrapper {
    private final int color;
    HeatOverlay(VertexConsumer parent, float heat) { super(parent); color = HeatGlowModel.color(heat); }
    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        parent.setColor((color >>> 16) & 255, (color >>> 8) & 255, color & 255, color >>> 24);
        return this;
    }
    @Override public VertexConsumer setUv2(int u, int v) {
        parent.setUv2(LightTexture.FULL_BRIGHT & 65535, LightTexture.FULL_BRIGHT >>> 16);
        return this;
    }
}
