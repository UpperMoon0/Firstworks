package com.nstut.firstworks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FirstworksConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REPLACE_ANIMAL_LEATHER = BUILDER
            .comment("Replace leather dropped directly by vanilla animals with Firstworks raw hide.",
                    "Disable this when a modpack manages hide acquisition through KubeJS or loot tables.")
            .define("replaceAnimalLeatherDrops", true);

    public static final ModConfigSpec.BooleanValue BIND_VANILLA_TOOL_RECIPES = BUILDER
            .comment("Require fibre bindings in vanilla wooden and stone tools, and rope in vanilla metal and diamond tools.",
                    "Disable this to retain vanilla tool recipes while keeping the Firstworks cordage materials available.",
                    "Changing this option requires a datapack reload or game restart.")
            .define("bindVanillaToolRecipes", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TEXTILE_PROGRESSION = BUILDER
            .comment("Replace sheep wool drops with colored fleece, remove String-to-Wool, and require Cloth and Clean Wool for beds.",
                    "Disable this to restore vanilla sheep, wool, and bed progression while keeping Firstworks textile items available.",
                    "Changing this option requires a datapack reload or game restart.")
            .define("enableTextileProgression", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FirstworksConfig() {}
}
