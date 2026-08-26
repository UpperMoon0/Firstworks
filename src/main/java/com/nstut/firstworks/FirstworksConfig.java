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

    public static final ModConfigSpec.BooleanValue ENABLE_MASONRY_PROGRESSION = BUILDER
            .comment("Require configurable-press unfired brick molding, brick firing, barrel mortar, and mortar-bound brick blocks for masonry.",
                    "Disable this to retain vanilla brick progression while keeping Firstworks masonry items available.",
                    "Changing this option requires a datapack reload or game restart.")
            .define("enableMasonryProgression", true);

    public static final ModConfigSpec.BooleanValue RAIN_FILLS_BARRELS = BUILDER
            .comment("Allow rain to naturally fill open Barrels that contain no fluid or only water.",
                    "Disabled by default to avoid changing existing-world behaviour.",
                    "Sealed Barrels and Barrels holding a non-water fluid are never filled by rain.",
                    "Cover or enclose a Barrel to keep it dry.")
            .define("rainFillsBarrels", false);

    public static final ModConfigSpec.IntValue RAIN_FILL_AMOUNT = BUILDER
            .comment("Water (in millibuckets) gathered from rain per precipitation event before being committed.",
                    "Water is always committed in 250 mB steps so it never leaves a residue that breaks fluid-exact recipes.",
                    "Only used when rainFillsBarrels is enabled.")
            .defineInRange("rainFillAmount", 100, 1, 4000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FirstworksConfig() {}
}
