package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.Firstworks;
import dev.latvian.mods.kubejs.registry.RegistryKubeEvent;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.KubeResourceLocation;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/** Startup-script global `Firstworks` exposing wood-variant registration helpers. */
public final class FirstworksKubeJS {
    public static final FirstworksKubeJS INSTANCE = new FirstworksKubeJS();

    private FirstworksKubeJS() {}

    @Info("""
            Registers functional Firstworks loom and barrel variants for a wood type.

            Required properties are `planks`, `slab`, and `log`. Optional properties are
            `strippedLog`, `plankTexture`, `logTexture`, `logTopTexture`,
            `strippedLogTexture`, `displayName`, and `recipes`.
            """)
    public void registerWoodType(Context context, RegistryKubeEvent<Block> event,
            KubeResourceLocation materialId, Map<?, ?> properties) {
        FirstworksWoodTypeBinding.registerWoodType(context, event, materialId, properties);
    }

    public ResourceLocation id(String path) {
        return Firstworks.id(path);
    }
}
