package com.nstut.firstworks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FirstworksConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REPLACE_ANIMAL_LEATHER = BUILDER
            .comment("Replace leather from entity types in #firstworks:leather_drops_as_raw_hide with Firstworks raw hide.",
                    "#firstworks:no_raw_hide_drops takes priority, allowing datapacks to exclude entries without replacing the default tag.",
                    "Disable this when a modpack manages hide acquisition entirely through loot tables.")
            .define("replaceAnimalLeatherDrops", true);

    public static final ModConfigSpec.BooleanValue ADD_ANIMAL_BONE_DROPS = BUILDER
            .comment("Add 1-2 bones to entities in #firstworks:drops_bones.",
                    "#firstworks:no_bone_drops takes priority, allowing datapacks to exclude entries without replacing the default tag.",
                    "The default list contains vertebrate animals and deliberately excludes squid, bees, allays, and undead horses.")
            .define("addAnimalBoneDrops", true);

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
            .comment("Allow rain to naturally fill open Barrels whose input store holds no fluid or only water.",
                    "Disabled by default to avoid changing existing-world behaviour.",
                    "Rain does not fill Barrels whose input store already contains a non-water fluid.",
                    "Existing output fluid is never modified. Cover or enclose a Barrel to keep it dry.")
            .define("rainFillsBarrels", true);

    public static final ModConfigSpec.IntValue RAIN_FILL_AMOUNT = BUILDER
            .comment("Water (in millibuckets) gathered from rain per precipitation event.",
                    "Only used when rainFillsBarrels is enabled.")
            .defineInRange("rainFillAmount", 100, 1, 4000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FirstworksConfig() {}
}
