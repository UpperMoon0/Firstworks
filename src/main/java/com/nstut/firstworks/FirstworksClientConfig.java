package com.nstut.firstworks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FirstworksClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue LOOM_ASSISTANCE = BUILDER
            .comment("Hold Use on the shuttle to weave without repeated aiming movements. Uses the same crossing and packing timing.")
            .define("loomInputAssistance", false);
    public static final ModConfigSpec.EnumValue<com.nstut.firstworks.content.workshop.TemperatureUnit> HEAT_UNIT = BUILDER
            .comment("Temperature display unit: CELSIUS, FAHRENHEIT, or KELVIN. Converts automatically; does not change heat or forging.")
            .defineEnum("heatTemperatureUnit", com.nstut.firstworks.content.workshop.TemperatureUnit.CELSIUS);
    public static final ModConfigSpec SPEC = BUILDER.build();
    private FirstworksClientConfig() {}
}
