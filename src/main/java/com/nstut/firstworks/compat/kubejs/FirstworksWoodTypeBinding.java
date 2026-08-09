package com.nstut.firstworks.compat.kubejs;

import com.nstut.firstworks.Firstworks;
import dev.latvian.mods.kubejs.registry.RegistryKubeEvent;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.util.KubeResourceLocation;
import dev.latvian.mods.rhino.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/** Startup-script helpers for registering a matched loom/barrel wood family. */
public final class FirstworksWoodTypeBinding {
    @Info("""
            Registers functional Firstworks loom and barrel variants for a wood type.

            Required properties are `planks`, `slab`, and `log`. Optional properties are
            `strippedLog`, `plankTexture`, `logTexture`, `logTopTexture`,
            `strippedLogTexture`, `displayName`, and `recipes`.
            """)
    public static void registerWoodType(Context context, RegistryKubeEvent<Block> event,
            KubeResourceLocation materialId, Map<?, ?> properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Firstworks wood type '" + materialId + "' requires a properties object");
        }

        ResourceLocation planks = requiredId(properties, "planks", materialId);
        ResourceLocation slab = requiredId(properties, "slab", materialId);
        ResourceLocation log = requiredId(properties, "log", materialId);
        ResourceLocation strippedLog = optionalId(properties, "strippedLog", log);

        ResourceLocation plankTexture = optionalId(properties, "plankTexture", blockTexture(planks));
        ResourceLocation logTexture = optionalId(properties, "logTexture", blockTexture(log));
        ResourceLocation logTopTexture = optionalId(properties, "logTopTexture", topTexture(logTexture));
        ResourceLocation strippedLogTexture = optionalId(properties, "strippedLogTexture", blockTexture(strippedLog));

        String displayName = optionalString(properties, "displayName", titleCase(materialId.wrapped().getPath()));
        boolean recipes = optionalBoolean(properties, "recipes", true);

        ResourceLocation loomId = materialId.wrapped().withPath(path -> path + "_loom");
        ResourceLocation barrelId = materialId.wrapped().withPath(path -> path + "_barrel");

        var loom = (FirstworksLoomBuilder) event.create(context, new KubeResourceLocation(loomId),
                new KubeResourceLocation(Firstworks.id("loom")));
        loom.configure(planks, slab, plankTexture, strippedLogTexture, displayName, recipes);

        var barrel = (FirstworksBarrelBuilder) event.create(context, new KubeResourceLocation(barrelId),
                new KubeResourceLocation(Firstworks.id("barrel")));
        barrel.configure(planks, slab, plankTexture, logTexture, logTopTexture, displayName, recipes);
    }

    private static ResourceLocation requiredId(Map<?, ?> properties, String key, KubeResourceLocation materialId) {
        Object value = properties.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Firstworks wood type '" + materialId + "' is missing required property '" + key + "'");
        }
        return ResourceLocation.parse(value.toString());
    }

    private static ResourceLocation optionalId(Map<?, ?> properties, String key, ResourceLocation fallback) {
        Object value = properties.get(key);
        return value == null || value.toString().isBlank() ? fallback : ResourceLocation.parse(value.toString());
    }

    private static String optionalString(Map<?, ?> properties, String key, String fallback) {
        Object value = properties.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private static boolean optionalBoolean(Map<?, ?> properties, String key, boolean fallback) {
        Object value = properties.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }

    private static ResourceLocation blockTexture(ResourceLocation blockId) {
        return blockId.withPath(path -> "block/" + path);
    }

    private static ResourceLocation topTexture(ResourceLocation sideTexture) {
        return sideTexture.withPath(path -> path + "_top");
    }

    private static String titleCase(String path) {
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '-' || c == '/') {
                result.append(' ');
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return result.toString();
    }

    private FirstworksWoodTypeBinding() {}
}
