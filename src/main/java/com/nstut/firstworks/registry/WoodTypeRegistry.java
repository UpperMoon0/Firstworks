package com.nstut.firstworks.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative mapping between a log block and the wood type used for Tree Bark.
 *
 * <p>Explicit registrations come from {@code Firstworks.registerWoodType(...)} (KubeJS). Anything
 * not explicitly registered falls back to a conventional inference from the log's registry name so
 * that modded {@code *_log}/{@code *_stem} blocks still yield correctly named bark.</p>
 */
public final class WoodTypeRegistry {
    private static final Map<ResourceLocation, Entry> BY_LOG = new HashMap<>();
    private static final Map<String, Entry> BY_WOOD_TYPE = new HashMap<>();

    public record Entry(String woodType, ResourceLocation logTexture, String displayName) {}

    public static void register(ResourceLocation logId, String woodType, ResourceLocation logTexture, String displayName) {
        Entry entry = new Entry(woodType, logTexture, displayName);
        BY_LOG.put(logId, entry);
        BY_WOOD_TYPE.put(woodType, entry);
    }

    public static Entry getByLog(ResourceLocation logId) {
        return BY_LOG.get(logId);
    }

    public static boolean isRegisteredLog(ResourceLocation logId) {
        return BY_LOG.containsKey(logId);
    }

    public static Entry getByWoodType(String woodType) {
        return BY_WOOD_TYPE.get(woodType);
    }

    public static ResourceLocation getLogTexture(String woodType) {
        Entry entry = BY_WOOD_TYPE.get(woodType);
        return entry == null ? null : entry.logTexture();
    }

    public static String getDisplayName(String woodType) {
        Entry entry = BY_WOOD_TYPE.get(woodType);
        return entry == null ? null : entry.displayName();
    }

    public static Set<String> explicitWoodTypes() {
        return new LinkedHashSet<>(BY_WOOD_TYPE.keySet());
    }

    /** Resolve the wood type for an (un)stripped log, preferring explicit registration. */
    public static String resolveWoodType(ResourceLocation logKey) {
        Entry entry = BY_LOG.get(logKey);
        if (entry != null) return entry.woodType();
        return inferWoodType(logKey);
    }

    /** Conventional inference for logs not explicitly registered through {@code registerWoodType}. */
    public static String inferWoodType(ResourceLocation key) {
        String path = key.getPath();
        for (String suffix : new String[]{ "_log", "_wood", "_stem", "_hyphae" }) {
            if (path.endsWith(suffix)) {
                String base = path.substring(0, path.length() - suffix.length());
                return key.getNamespace().equals("minecraft") ? base : key.getNamespace() + ":" + base;
            }
        }
        if (key.getNamespace().equals("minecraft") && path.equals("bamboo_block")) {
            return "bamboo";
        }
        return "oak";
    }

    private WoodTypeRegistry() {}
}
