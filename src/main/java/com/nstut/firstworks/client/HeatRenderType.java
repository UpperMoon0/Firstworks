package com.nstut.firstworks.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Additive emission with source alpha: transparent sprite pixels never emit a rectangular halo. */
final class HeatRenderType extends RenderType {
    static final RenderType GLOW = glow("firstworks_heat", true);
    // GUI item poses sit in front of the camera; view-offset scaling moves them behind the base sprite.
    static final RenderType GUI_GLOW = glow("firstworks_heat_gui", false);
    private static RenderType glow(String name, boolean viewOffset) {
        return create(name, DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLayeringState(viewOffset ? VIEW_OFFSET_Z_LAYERING : NO_LAYERING)
                    .createCompositeState(false));
    }
    private HeatRenderType() {
        super("firstworks_heat", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                1536, false, true, () -> {}, () -> {});
    }
}
