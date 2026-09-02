package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.ClayBucketItem;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.HandSpindleItem;
import com.nstut.firstworks.content.KnifeItem;
import com.nstut.firstworks.content.TanninSolutionBucketItem;
import com.nstut.firstworks.content.TreeBarkItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, Firstworks.MOD_ID);

    // Stone-age materials and tools.
    public static final DeferredHolder<Item, Item> TREE_BARK = ITEMS.register(
            "tree_bark", () -> new TreeBarkItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_HIDE = simple("raw_hide");
    public static final DeferredHolder<Item, Item> SOAKED_HIDE = simple("soaked_hide");
    public static final DeferredHolder<Item, Item> SCRAPED_HIDE = simple("scraped_hide");
    public static final DeferredHolder<Item, Item> TANNIN_SOAKED_HIDE = simple("tannin_soaked_hide");
    public static final DeferredHolder<Item, Item> PLANT_FIBRE = simple("plant_fibre");
    public static final DeferredHolder<Item, Item> CRUDE_CORDAGE = simple("crude_cordage");
    public static final DeferredHolder<Item, Item> FIRE_STARTER = ITEMS.register(
            "fire_starter", () -> new FlintAndSteelItem(new Item.Properties().durability(1)));
    public static final DeferredHolder<Item, Item> UNFIRED_CLAY_BUCKET = simple("unfired_clay_bucket");
    public static final DeferredHolder<Item, Item> UNFIRED_CLAY_BRICK = simple("unfired_clay_brick");
    public static final DeferredHolder<Item, Item> MORTAR = simple("mortar");
    public static final DeferredHolder<Item, Item> BRICK_MOLD = blockItem("brick_mold", ModBlocks.BRICK_MOLD);
    public static final DeferredHolder<Item, Item> STANDING_TORCH = blockItem("standing_torch", ModBlocks.STANDING_TORCH);
    public static final DeferredHolder<Item, Item> CLAY_BUCKET = ITEMS.register(
            "clay_bucket",
            () -> new ClayBucketItem(Fluids.EMPTY, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WATER_CLAY_BUCKET = ITEMS.register(
            "water_clay_bucket",
            () -> new ClayBucketItem(Fluids.WATER,
                    new Item.Properties().craftRemainder(CLAY_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> TANNIN_CLAY_BUCKET = ITEMS.register(
            "tannin_clay_bucket",
            () -> new ClayBucketItem(ModFluids.TANNIN_SOLUTION.get(),
                    new Item.Properties().craftRemainder(CLAY_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> RETTED_FIBRE = simple("retted_fibre");
    public static final DeferredHolder<Item, Item> HAND_SPINDLE = ITEMS.register(
            "hand_spindle", () -> new HandSpindleItem(new Item.Properties().durability(128)));
    public static final DeferredHolder<Item, Item> TWINE = simple("twine");
    public static final DeferredHolder<Item, Item> ROPE = simple("rope");
    public static final DeferredHolder<Item, Item> CLOTH = simple("cloth");
    public static final DeferredHolder<Item, Item> RAW_FLEECE = ITEMS.register(
            "raw_fleece", () -> new ColoredFleeceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLEAN_WOOL = ITEMS.register(
            "clean_wool", () -> new ColoredFleeceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BASKET = blockItem("basket", ModBlocks.BASKET);
    public static final DeferredHolder<Item, Item> COPPER_FASTENERS = simple("copper_fasteners");
    public static final DeferredHolder<Item, Item> MORTAR_AND_PESTLE =
            blockItem("mortar_and_pestle", ModBlocks.MORTAR_AND_PESTLE);
    public static final DeferredHolder<Item, Item> CHARCOAL_PILE = blockItem("charcoal_pile", ModBlocks.CHARCOAL_PILE);
    public static final DeferredHolder<Item, Item> CHARCOAL_POWDER = simple("charcoal_powder");
    public static final DeferredHolder<Item, Item> RAW_OCHRE = simple("raw_ochre");
    public static final DeferredHolder<Item, Item> GROUND_OCHRE = simple("ground_ochre");
    public static final DeferredHolder<Item, Item> FLOUR = simple("flour");
    public static final DeferredHolder<Item, Item> DOUGH = simple("dough");
    public static final DeferredHolder<Item, Item> QUERN = blockItem("quern", ModBlocks.QUERN);
    public static final DeferredHolder<Item, Item> TANNIN_SOLUTION_BUCKET = ITEMS.register(
            "tannin_solution_bucket",
            () -> new TanninSolutionBucketItem(ModFluids.TANNIN_SOLUTION.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // Stone completion and workshop ceramics.
    public static final DeferredHolder<Item, Item> RESIN = simple("resin");
    public static final DeferredHolder<Item, Item> RESIN_TAP = ITEMS.register(
            "resin_tap", () -> new Item(new Item.Properties().durability(96).stacksTo(1)));
    public static final DeferredHolder<Item, Item> HAFTING_COMPOUND = simple("hafting_compound");
    public static final DeferredHolder<Item, Item> GROG = simple("grog");
    public static final DeferredHolder<Item, Item> REFRACTORY_CLAY = simple("refractory_clay");
    public static final DeferredHolder<Item, Item> UNFIRED_REFRACTORY_BRICK = simple("unfired_refractory_brick");
    public static final DeferredHolder<Item, Item> REFRACTORY_BRICK = simple("refractory_brick");
    public static final DeferredHolder<Item, Item> UNFIRED_CRUCIBLE = simple("unfired_crucible");
    public static final DeferredHolder<Item, Item> CRUCIBLE = simple("crucible");
    public static final DeferredHolder<Item, Item> UNFIRED_TUYERE = simple("unfired_tuyere");
    public static final DeferredHolder<Item, Item> TUYERE = simple("tuyere");
    public static final DeferredHolder<Item, Item> UNFIRED_CASTING_MOLD = simple("unfired_casting_mold");
    public static final DeferredHolder<Item, Item> CASTING_MOLD = simple("casting_mold");
    public static final DeferredHolder<Item, Item> HEAVY_LEATHER = simple("heavy_leather");
    public static final DeferredHolder<Item, Item> STONE_HAMMER = ITEMS.register(
            "stone_hammer", () -> new Item(new Item.Properties().durability(192).stacksTo(1)));

    public static final DeferredHolder<Item, Item> POTTERY_WHEEL = blockItem("pottery_wheel", ModBlocks.POTTERY_WHEEL);
    public static final DeferredHolder<Item, Item> KILN = blockItem("kiln", ModBlocks.KILN);
    public static final DeferredHolder<Item, Item> STONE_ANVIL = blockItem("stone_anvil", ModBlocks.STONE_ANVIL);
    public static final DeferredHolder<Item, Item> BELLOWS = blockItem("bellows", ModBlocks.BELLOWS);
    public static final DeferredHolder<Item, Item> CRUCIBLE_FURNACE =
            blockItem("crucible_furnace", ModBlocks.CRUCIBLE_FURNACE);

    // Primitive copper and copper workshop upgrades.
    public static final DeferredHolder<Item, Item> CAST_COPPER_BILLET = simple("cast_copper_billet");
    public static final DeferredHolder<Item, Item> ANNEALED_COPPER_BILLET = simple("annealed_copper_billet");
    public static final DeferredHolder<Item, Item> WORKED_COPPER_BILLET = simple("worked_copper_billet");
    public static final DeferredHolder<Item, Item> COPPER_WIRE = simple("copper_wire");
    public static final DeferredHolder<Item, Item> COPPER_HAND_SPINDLE = ITEMS.register(
            "copper_hand_spindle",
            () -> new HandSpindleItem(new Item.Properties().durability(384), 0.6F));
    public static final DeferredHolder<Item, Item> COPPER_LOOM = blockItem("copper_loom", ModBlocks.COPPER_LOOM);
    public static final DeferredHolder<Item, Item> ROTARY_QUERN = blockItem("rotary_quern", ModBlocks.ROTARY_QUERN);
    public static final DeferredHolder<Item, Item> LIME = simple("lime");
    public static final DeferredHolder<Item, Item> PLASTER = simple("plaster");
    public static final DeferredHolder<Item, Item> PLASTER_BLOCK = blockItem("plaster_block", ModBlocks.PLASTER_BLOCK);

    // Tool tiers.
    public static final DeferredHolder<Item, Item> BONE_PICKAXE = pickaxe("bone_pickaxe", ModToolTiers.BONE, 1.0F, -2.8F);
    public static final DeferredHolder<Item, Item> BONE_AXE = axe("bone_axe", ModToolTiers.BONE, 7.0F, -3.2F);
    public static final DeferredHolder<Item, Item> BONE_SHOVEL = shovel("bone_shovel", ModToolTiers.BONE, 1.5F, -3.0F);
    public static final DeferredHolder<Item, Item> BONE_HOE = hoe("bone_hoe", ModToolTiers.BONE, -1.0F, -2.0F);
    public static final DeferredHolder<Item, Item> BONE_SWORD = sword("bone_sword", ModToolTiers.BONE, 3, -2.4F);
    public static final DeferredHolder<Item, Item> BONE_KNIFE = knife("bone_knife", ModToolTiers.BONE, 1, -2.0F);

    public static final DeferredHolder<Item, Item> FLINT_PICKAXE = pickaxe("flint_pickaxe", ModToolTiers.FLINT, 1.0F, -2.8F);
    public static final DeferredHolder<Item, Item> FLINT_AXE = axe("flint_axe", ModToolTiers.FLINT, 7.0F, -3.2F);
    public static final DeferredHolder<Item, Item> FLINT_SHOVEL = shovel("flint_shovel", ModToolTiers.FLINT, 1.5F, -3.0F);
    public static final DeferredHolder<Item, Item> FLINT_HOE = hoe("flint_hoe", ModToolTiers.FLINT, -1.0F, -2.0F);
    public static final DeferredHolder<Item, Item> FLINT_SWORD = sword("flint_sword", ModToolTiers.FLINT, 3, -2.4F);
    public static final DeferredHolder<Item, Item> FLINT_KNIFE = knife("flint_knife", ModToolTiers.FLINT, 1, -1.8F);

    public static final DeferredHolder<Item, Item> COPPER_KNIFE = knife("copper_knife", ModToolTiers.COPPER, 1, -1.8F);

    public static final Map<String, DeferredHolder<Item, Item>> BARREL_ITEMS = registerBarrelItems();
    public static final DeferredHolder<Item, Item> BARREL = BARREL_ITEMS.get("barrel");
    public static final Map<String, DeferredHolder<Item, Item>> LOOM_ITEMS = registerLoomItems();
    public static final DeferredHolder<Item, Item> LOOM = LOOM_ITEMS.get("loom");

    private static Map<String, DeferredHolder<Item, Item>> registerBarrelItems() {
        Map<String, DeferredHolder<Item, Item>> items = new LinkedHashMap<>();
        ModBlocks.BARRELS.forEach((name, block) -> items.put(name,
                ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()))));
        return items;
    }

    private static Map<String, DeferredHolder<Item, Item>> registerLoomItems() {
        Map<String, DeferredHolder<Item, Item>> items = new LinkedHashMap<>();
        ModBlocks.LOOMS.forEach((name, block) -> items.put(name,
                ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()))));
        return items;
    }

    private static DeferredHolder<Item, Item> blockItem(String name, DeferredHolder<Block, ? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> pickaxe(String name, Tier tier, float damage, float speed) {
        return ITEMS.register(name, () -> new PickaxeItem(tier,
                new Item.Properties().attributes(PickaxeItem.createAttributes(tier, damage, speed))));
    }

    private static DeferredHolder<Item, Item> axe(String name, Tier tier, float damage, float speed) {
        return ITEMS.register(name, () -> new AxeItem(tier,
                new Item.Properties().attributes(AxeItem.createAttributes(tier, damage, speed))));
    }

    private static DeferredHolder<Item, Item> shovel(String name, Tier tier, float damage, float speed) {
        return ITEMS.register(name, () -> new ShovelItem(tier,
                new Item.Properties().attributes(ShovelItem.createAttributes(tier, damage, speed))));
    }

    private static DeferredHolder<Item, Item> hoe(String name, Tier tier, float damage, float speed) {
        return ITEMS.register(name, () -> new HoeItem(tier,
                new Item.Properties().attributes(HoeItem.createAttributes(tier, damage, speed))));
    }

    private static DeferredHolder<Item, Item> sword(String name, Tier tier, int damage, float speed) {
        return ITEMS.register(name, () -> new SwordItem(tier,
                new Item.Properties().attributes(SwordItem.createAttributes(tier, damage, speed))));
    }

    private static DeferredHolder<Item, Item> knife(String name, Tier tier, int damage, float speed) {
        return ITEMS.register(name, () -> new KnifeItem(tier, damage, speed));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
