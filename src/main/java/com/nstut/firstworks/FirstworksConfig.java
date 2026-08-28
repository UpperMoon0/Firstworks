package com.nstut.firstworks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FirstworksConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REPLACE_ANIMAL_LEATHER = BUILDER
            .comment("Replace leather from entity types in #firstworks:leather_drops_as_raw_hide with Firstworks raw hide,",
                    "and rewrite vanilla's 4-rabbit-hide crafting recipe to yield raw hide instead of finished leather.",
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
    public static final ModConfigSpec.BooleanValue BIND_PRIMITIVE_VANILLA_TOOLS = BUILDER
            .comment("Require #firstworks:primitive_bindings for vanilla wooden and stone tools.")
            .define("bindPrimitiveVanillaTools", true);
    public static final ModConfigSpec.BooleanValue BIND_METAL_VANILLA_TOOLS = BUILDER
            .comment("Require #firstworks:strong_bindings for vanilla iron, gold, and diamond tools.")
            .define("bindMetalVanillaTools", true);

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
                    "Enabled by default; disable to keep open Barrels dry unless filled manually.",
                    "Rain does not fill Barrels whose input store already contains a non-water fluid.",
                    "Existing output fluid is never modified. Cover or enclose a Barrel to keep it dry.")
            .define("rainFillsBarrels", true);

    public static final ModConfigSpec.IntValue RAIN_FILL_AMOUNT = BUILDER
            .comment("Water (in millibuckets) gathered from rain per precipitation event.",
                    "Only used when rainFillsBarrels is enabled.")
            .defineInRange("rainFillAmount", 100, 1, 4000);

    public static final ModConfigSpec.IntValue CHARCOAL_CARBONIZE_DURATION = BUILDER
            .comment("Ticks required for an earthen charcoal mound to carbonize (default: 6000 ticks / 5 minutes).")
            .defineInRange("charcoalCarbonizeDuration", 6000, 20, 72000);

    public static final ModConfigSpec.IntValue CHARCOAL_MAX_LOGS = BUILDER
            .comment("Maximum number of connected logs permitted in a single charcoal mound.")
            .defineInRange("charcoalMaxLogs", 64, 4, 256);

    public static final ModConfigSpec.DoubleValue CHARCOAL_NORMAL_YIELD = BUILDER
            .comment("Charcoal yield multiplier when the mound carbonizes without being breached.")
            .defineInRange("charcoalNormalYield", 0.75D, 0.05D, 1.0D);

    public static final ModConfigSpec.DoubleValue CHARCOAL_BREACHED_YIELD = BUILDER
            .comment("Charcoal yield multiplier if the mound is opened before carbonization completes.")
            .defineInRange("charcoalBreachedYield", 0.25D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue PLANT_FIBRE_HAND_CHANCE = BUILDER
            .comment("Chance (0.0 to 1.0) to gather Plant Fibre with an empty hand or non-knife tool.",
                    "Default: 0.30",
                    "Range: 0.0 ~ 1.0")
            .defineInRange("plantFibreHandChance", 0.30D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private FirstworksConfig() {}
}
