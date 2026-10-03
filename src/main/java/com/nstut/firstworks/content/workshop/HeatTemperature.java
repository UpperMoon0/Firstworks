package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.FirstworksConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Temperature scale for item heat; cooling duration and forge readiness remain authoritative. */
public final class HeatTemperature {
    public static final int AMBIENT_CELSIUS = 20;
    public static boolean validOverride(Object value) {
        if (!(value instanceof String entry)) return false;
        String[] parts = entry.split("=", -1);
        if (parts.length != 2 || ResourceLocation.tryParse(parts[0].trim()) == null) return false;
        try {
            int maximum = Integer.parseInt(parts[1].trim());
            return maximum > AMBIENT_CELSIUS && maximum <= 5000;
        } catch (NumberFormatException e) { return false; }
    }
    public static int maximum(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int maximum = FirstworksConfig.DEFAULT_MAX_HEAT_CELSIUS.get();
        for (String entry : FirstworksConfig.ITEM_MAX_HEAT_CELSIUS.get()) {
            String[] parts = entry.split("=", -1);
            if (validOverride(entry) && parts[0].trim().equals(id)) maximum = Integer.parseInt(parts[1].trim());
        }
        return maximum;
    }
    public static double celsius(float fraction, int maximum) {
        return AMBIENT_CELSIUS + Math.max(0, Math.min(1, fraction)) * (maximum - AMBIENT_CELSIUS);
    }
    public static int workableSeconds(int remaining, int capacity) {
        int minimum = (capacity + 3) / 4;
        return Math.max(0, (remaining - minimum + 19) / 20);
    }
    private HeatTemperature() {}
}
