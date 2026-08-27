package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.TreeBarkItem;
import com.nstut.firstworks.content.TanninSolutionBucketItem;
import com.nstut.firstworks.content.ColoredFleeceItem;
import com.nstut.firstworks.content.ClayBucketItem;
import com.nstut.firstworks.content.HandSpindleItem;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Firstworks.MOD_ID);

    public static final DeferredHolder<Item, Item> TREE_BARK = ITEMS.register("tree_bark",
            () -> new TreeBarkItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_HIDE = simple("raw_hide");
    public static final DeferredHolder<Item, Item> SOAKED_HIDE = simple("soaked_hide");
    public static final DeferredHolder<Item, Item> SCRAPED_HIDE = simple("scraped_hide");
    public static final DeferredHolder<Item, Item> TANNIN_SOAKED_HIDE = simple("tannin_soaked_hide");
    public static final DeferredHolder<Item, Item> PLANT_FIBRE = simple("plant_fibre");
    public static final DeferredHolder<Item, Item> CRUDE_CORDAGE = simple("crude_cordage");
    public static final DeferredHolder<Item, Item> FIRE_STARTER = ITEMS.register("fire_starter",
            () -> new FlintAndSteelItem(new Item.Properties().durability(1)));
    public static final DeferredHolder<Item, Item> UNFIRED_CLAY_BUCKET = simple("unfired_clay_bucket");
    public static final DeferredHolder<Item, Item> UNFIRED_CLAY_BRICK = simple("unfired_clay_brick");
    public static final DeferredHolder<Item, Item> MORTAR = simple("mortar");
    public static final DeferredHolder<Item, Item> BRICK_MOLD = ITEMS.register("brick_mold",
            () -> new BlockItem(ModBlocks.BRICK_MOLD.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDING_TORCH = ITEMS.register("standing_torch",
            () -> new BlockItem(ModBlocks.STANDING_TORCH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLAY_BUCKET = ITEMS.register("clay_bucket",
            () -> new ClayBucketItem(Fluids.EMPTY, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WATER_CLAY_BUCKET = ITEMS.register("water_clay_bucket",
            () -> new ClayBucketItem(Fluids.WATER, new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> TANNIN_CLAY_BUCKET = ITEMS.register("tannin_clay_bucket",
            () -> new ClayBucketItem(ModFluids.TANNIN_SOLUTION.get(),
                    new Item.Properties().craftRemainder(CLAY_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> RETTED_FIBRE = simple("retted_fibre");
    public static final DeferredHolder<Item, Item> HAND_SPINDLE = ITEMS.register("hand_spindle",
            () -> new HandSpindleItem(new Item.Properties().durability(128)));
    public static final DeferredHolder<Item, Item> TWINE = simple("twine");
    public static final DeferredHolder<Item, Item> ROPE = simple("rope");
    public static final DeferredHolder<Item, Item> CLOTH = simple("cloth");
    public static final DeferredHolder<Item, Item> RAW_FLEECE = ITEMS.register("raw_fleece",
            () -> new ColoredFleeceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLEAN_WOOL = ITEMS.register("clean_wool",
            () -> new ColoredFleeceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TANNIN_SOLUTION_BUCKET = ITEMS.register("tannin_solution_bucket",
            () -> new TanninSolutionBucketItem(ModFluids.TANNIN_SOLUTION.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // Bone tools
    public static final DeferredHolder<Item, Item> BONE_PICKAXE = ITEMS.register("bone_pickaxe",
            () -> new PickaxeItem(ModToolTiers.BONE, new Item.Properties().attributes(PickaxeItem.createAttributes(ModToolTiers.BONE, 1.0F, -2.8F))));
    public static final DeferredHolder<Item, Item> BONE_AXE = ITEMS.register("bone_axe",
            () -> new AxeItem(ModToolTiers.BONE, new Item.Properties().attributes(AxeItem.createAttributes(ModToolTiers.BONE, 7.0F, -3.2F))));
    public static final DeferredHolder<Item, Item> BONE_SHOVEL = ITEMS.register("bone_shovel",
            () -> new ShovelItem(ModToolTiers.BONE, new Item.Properties().attributes(ShovelItem.createAttributes(ModToolTiers.BONE, 1.5F, -3.0F))));
    public static final DeferredHolder<Item, Item> BONE_HOE = ITEMS.register("bone_hoe",
            () -> new HoeItem(ModToolTiers.BONE, new Item.Properties().attributes(HoeItem.createAttributes(ModToolTiers.BONE, -1.0F, -2.0F))));
    public static final DeferredHolder<Item, Item> BONE_SWORD = ITEMS.register("bone_sword",
            () -> new SwordItem(ModToolTiers.BONE, new Item.Properties().attributes(SwordItem.createAttributes(ModToolTiers.BONE, 3, -2.4F))));

    // Flint tools
    public static final DeferredHolder<Item, Item> FLINT_PICKAXE = ITEMS.register("flint_pickaxe",
            () -> new PickaxeItem(ModToolTiers.FLINT, new Item.Properties().attributes(PickaxeItem.createAttributes(ModToolTiers.FLINT, 1.0F, -2.8F))));
    public static final DeferredHolder<Item, Item> FLINT_AXE = ITEMS.register("flint_axe",
            () -> new AxeItem(ModToolTiers.FLINT, new Item.Properties().attributes(AxeItem.createAttributes(ModToolTiers.FLINT, 7.0F, -3.2F))));
    public static final DeferredHolder<Item, Item> FLINT_SHOVEL = ITEMS.register("flint_shovel",
            () -> new ShovelItem(ModToolTiers.FLINT, new Item.Properties().attributes(ShovelItem.createAttributes(ModToolTiers.FLINT, 1.5F, -3.0F))));
    public static final DeferredHolder<Item, Item> FLINT_HOE = ITEMS.register("flint_hoe",
            () -> new HoeItem(ModToolTiers.FLINT, new Item.Properties().attributes(HoeItem.createAttributes(ModToolTiers.FLINT, -1.0F, -2.0F))));
    public static final DeferredHolder<Item, Item> FLINT_SWORD = ITEMS.register("flint_sword",
            () -> new SwordItem(ModToolTiers.FLINT, new Item.Properties().attributes(SwordItem.createAttributes(ModToolTiers.FLINT, 3, -2.4F))));

    public static final Map<String, DeferredHolder<Item, Item>> BARREL_ITEMS = registerBarrelItems();
    public static final DeferredHolder<Item, Item> BARREL = BARREL_ITEMS.get("barrel");
    public static final Map<String, DeferredHolder<Item, Item>> LOOM_ITEMS = registerLoomItems();
    public static final DeferredHolder<Item, Item> LOOM = LOOM_ITEMS.get("loom");

    private static Map<String, DeferredHolder<Item, Item>> registerBarrelItems() {
        Map<String, DeferredHolder<Item, Item>> items = new LinkedHashMap<>();
        ModBlocks.BARRELS.forEach((name, block) -> items.put(name, ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()))));
        return items;
    }



    private static Map<String, DeferredHolder<Item, Item>> registerLoomItems() {
        Map<String, DeferredHolder<Item, Item>> items = new LinkedHashMap<>();
        ModBlocks.LOOMS.forEach((name, block) -> items.put(name, ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties()))));
        return items;
    }

    private static DeferredHolder<Item, Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {}
}
