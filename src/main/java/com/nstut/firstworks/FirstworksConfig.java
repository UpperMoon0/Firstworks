package com.nstut.firstworks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FirstworksConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REPLACE_ANIMAL_LEATHER = BUILDER
            .comment("Replace leather dropped directly by vanilla animals with Firstworks raw hide.",
                    "Disable this when a modpack manages hide acquisition through KubeJS or loot tables.")
            .define("replaceAnimalLeatherDrops", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FirstworksConfig() {}
}
