package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Firstworks.MOD_ID);
    public static final DeferredHolder<Item, Item> TREE_BARK=ITEMS.register("tree_bark",()->new TreeBarkItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_HIDE=simple("raw_hide"),SOAKED_HIDE=simple("soaked_hide"),SCRAPED_HIDE=simple("scraped_hide"),TANNIN_SOAKED_HIDE=simple("tannin_soaked_hide"),PLANT_FIBRE=simple("plant_fibre"),CRUDE_CORDAGE=simple("crude_cordage");
    public static final DeferredHolder<Item, Item> FIRE_STARTER=ITEMS.register("fire_starter",()->new FlintAndSteelItem(new Item.Properties().durability(1)));
    public static final DeferredHolder<Item, Item> UNFIRED_CLAY_BUCKET=simple("unfired_clay_bucket"),UNFIRED_CLAY_BRICK=simple("unfired_clay_brick"),MORTAR=simple("mortar");
    public static final DeferredHolder<Item, Item> BRICK_MOLD=blockItem("brick_mold",ModBlocks.BRICK_MOLD),STANDING_TORCH=blockItem("standing_torch",ModBlocks.STANDING_TORCH);
    public static final DeferredHolder<Item, Item> CLAY_BUCKET=ITEMS.register("clay_bucket",()->new ClayBucketItem(Fluids.EMPTY,new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WATER_CLAY_BUCKET=ITEMS.register("water_clay_bucket",()->new ClayBucketItem(Fluids.WATER,new Item.Properties().craftRemainder(CLAY_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> TANNIN_CLAY_BUCKET=ITEMS.register("tannin_clay_bucket",()->new ClayBucketItem(ModFluids.TANNIN_SOLUTION.get(),new Item.Properties().craftRemainder(CLAY_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> RETTED_FIBRE=simple("retted_fibre");
    public static final DeferredHolder<Item, Item> HAND_SPINDLE=ITEMS.register("hand_spindle",()->new HandSpindleItem(new Item.Properties().durability(128)));
    public static final DeferredHolder<Item, Item> TWINE=simple("twine"),ROPE=simple("rope"),CLOTH=simple("cloth");
    public static final DeferredHolder<Item, Item> RAW_FLEECE=ITEMS.register("raw_fleece",()->new ColoredFleeceItem(new Item.Properties())),CLEAN_WOOL=ITEMS.register("clean_wool",()->new ColoredFleeceItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BASKET=blockItem("basket",ModBlocks.BASKET),COPPER_FASTENERS=simple("copper_fasteners"),MORTAR_AND_PESTLE=blockItem("mortar_and_pestle",ModBlocks.MORTAR_AND_PESTLE),CHARCOAL_PILE=blockItem("charcoal_pile",ModBlocks.CHARCOAL_PILE);
    public static final DeferredHolder<Item, Item> CHARCOAL_POWDER=simple("charcoal_powder"),RAW_OCHRE=simple("raw_ochre"),GROUND_OCHRE=simple("ground_ochre"),FLOUR=simple("flour"),DOUGH=simple("dough");
    public static final DeferredHolder<Item, Item> QUERN=blockItem("quern",ModBlocks.QUERN);
    public static final DeferredHolder<Item, Item> TANNIN_SOLUTION_BUCKET=ITEMS.register("tannin_solution_bucket",()->new TanninSolutionBucketItem(ModFluids.TANNIN_SOLUTION.get(),new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final DeferredHolder<Item, Item> RESIN=simple("resin"),RESIN_TAP=ITEMS.register("resin_tap",()->new Item(new Item.Properties().durability(96).stacksTo(1))),HAFTING_COMPOUND=simple("hafting_compound");
    public static final DeferredHolder<Item, Item> GROG=simple("grog"),REFRACTORY_CLAY=simple("refractory_clay"),UNFIRED_REFRACTORY_BRICK=simple("unfired_refractory_brick"),REFRACTORY_BRICK=simple("refractory_brick"),UNFIRED_CRUCIBLE=simple("unfired_crucible"),CRUCIBLE=simple("crucible"),UNFIRED_TUYERE=simple("unfired_tuyere"),TUYERE=simple("tuyere"),UNFIRED_CASTING_MOLD=simple("unfired_casting_mold"),CASTING_MOLD=simple("casting_mold"),HEAVY_LEATHER=simple("heavy_leather");
    public static final DeferredHolder<Item, Item> STONE_HAMMER=ITEMS.register("stone_hammer",()->new Item(new Item.Properties().durability(192).stacksTo(1)));
    public static final DeferredHolder<Item, Item> POTTERY_WHEEL=blockItem("pottery_wheel",ModBlocks.POTTERY_WHEEL),KILN=blockItem("kiln",ModBlocks.KILN),STONE_ANVIL=blockItem("stone_anvil",ModBlocks.STONE_ANVIL),BELLOWS=blockItem("bellows",ModBlocks.BELLOWS),CRUCIBLE_FURNACE=blockItem("crucible_furnace",ModBlocks.CRUCIBLE_FURNACE);
    public static final DeferredHolder<Item, Item> CAST_COPPER_BILLET=simple("cast_copper_billet"),ANNEALED_COPPER_BILLET=simple("annealed_copper_billet"),WORKED_COPPER_BILLET=simple("worked_copper_billet"),COPPER_WIRE=simple("copper_wire");
    public static final DeferredHolder<Item, Item> COPPER_HAND_SPINDLE=ITEMS.register("copper_hand_spindle",()->new HandSpindleItem(new Item.Properties().durability(384),.6F));
    public static final DeferredHolder<Item, Item> COPPER_LOOM=blockItem("copper_loom",ModBlocks.COPPER_LOOM),ROTARY_QUERN=blockItem("rotary_quern",ModBlocks.ROTARY_QUERN);
    public static final DeferredHolder<Item, Item> LIME=simple("lime"),PLASTER=simple("plaster"),PLASTER_BLOCK=blockItem("plaster_block",ModBlocks.PLASTER_BLOCK);
    public static final DeferredHolder<Item, Item> COPPER_SHEARS=ITEMS.register("copper_shears",()->new ShearsItem(new Item.Properties().durability(180)));
    public static final DeferredHolder<Item, Item> COPPER_BUCKET=ITEMS.register("copper_bucket",()->new CopperBucketItem(Fluids.EMPTY,new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WATER_COPPER_BUCKET=ITEMS.register("water_copper_bucket",()->new CopperBucketItem(Fluids.WATER,new Item.Properties().craftRemainder(COPPER_BUCKET.get()).stacksTo(1)));
    public static final DeferredHolder<Item, Item> TANNIN_COPPER_BUCKET=ITEMS.register("tannin_copper_bucket",()->new CopperBucketItem(ModFluids.TANNIN_SOLUTION.get(),new Item.Properties().craftRemainder(COPPER_BUCKET.get()).stacksTo(1)));

    public static final DeferredHolder<Item, Item> BONE_PICKAXE=ITEMS.register("bone_pickaxe",()->new PickaxeItem(ModToolTiers.BONE,new Item.Properties().attributes(PickaxeItem.createAttributes(ModToolTiers.BONE,1F,-2.8F)))),BONE_AXE=ITEMS.register("bone_axe",()->new AxeItem(ModToolTiers.BONE,new Item.Properties().attributes(AxeItem.createAttributes(ModToolTiers.BONE,7F,-3.2F)))),BONE_SHOVEL=ITEMS.register("bone_shovel",()->new ShovelItem(ModToolTiers.BONE,new Item.Properties().attributes(ShovelItem.createAttributes(ModToolTiers.BONE,1.5F,-3F)))),BONE_HOE=ITEMS.register("bone_hoe",()->new HoeItem(ModToolTiers.BONE,new Item.Properties().attributes(HoeItem.createAttributes(ModToolTiers.BONE,-1F,-2F)))),BONE_SWORD=ITEMS.register("bone_sword",()->new SwordItem(ModToolTiers.BONE,new Item.Properties().attributes(SwordItem.createAttributes(ModToolTiers.BONE,3,-2.4F)))),BONE_KNIFE=ITEMS.register("bone_knife",()->new KnifeItem(ModToolTiers.BONE,1,-2F));
    public static final DeferredHolder<Item, Item> FLINT_PICKAXE=ITEMS.register("flint_pickaxe",()->new PickaxeItem(ModToolTiers.FLINT,new Item.Properties().attributes(PickaxeItem.createAttributes(ModToolTiers.FLINT,1F,-2.8F)))),FLINT_AXE=ITEMS.register("flint_axe",()->new AxeItem(ModToolTiers.FLINT,new Item.Properties().attributes(AxeItem.createAttributes(ModToolTiers.FLINT,7F,-3.2F)))),FLINT_SHOVEL=ITEMS.register("flint_shovel",()->new ShovelItem(ModToolTiers.FLINT,new Item.Properties().attributes(ShovelItem.createAttributes(ModToolTiers.FLINT,1.5F,-3F)))),FLINT_HOE=ITEMS.register("flint_hoe",()->new HoeItem(ModToolTiers.FLINT,new Item.Properties().attributes(HoeItem.createAttributes(ModToolTiers.FLINT,-1F,-2F)))),FLINT_SWORD=ITEMS.register("flint_sword",()->new SwordItem(ModToolTiers.FLINT,new Item.Properties().attributes(SwordItem.createAttributes(ModToolTiers.FLINT,3,-2.4F)))),FLINT_KNIFE=ITEMS.register("flint_knife",()->new KnifeItem(ModToolTiers.FLINT,1,-1.8F));
    public static final DeferredHolder<Item, Item> COPPER_PICKAXE=ITEMS.register("copper_pickaxe",()->new PickaxeItem(ModToolTiers.COPPER,new Item.Properties().attributes(PickaxeItem.createAttributes(ModToolTiers.COPPER,1F,-2.8F)))),COPPER_AXE=ITEMS.register("copper_axe",()->new AxeItem(ModToolTiers.COPPER,new Item.Properties().attributes(AxeItem.createAttributes(ModToolTiers.COPPER,7F,-3.15F)))),COPPER_SHOVEL=ITEMS.register("copper_shovel",()->new ShovelItem(ModToolTiers.COPPER,new Item.Properties().attributes(ShovelItem.createAttributes(ModToolTiers.COPPER,1.5F,-3F)))),COPPER_HOE=ITEMS.register("copper_hoe",()->new HoeItem(ModToolTiers.COPPER,new Item.Properties().attributes(HoeItem.createAttributes(ModToolTiers.COPPER,-1F,-2F)))),COPPER_SWORD=ITEMS.register("copper_sword",()->new SwordItem(ModToolTiers.COPPER,new Item.Properties().attributes(SwordItem.createAttributes(ModToolTiers.COPPER,3,-2.4F)))),COPPER_KNIFE=ITEMS.register("copper_knife",()->new KnifeItem(ModToolTiers.COPPER,1,-1.8F));

    public static final Map<String,DeferredHolder<Item,Item>> BARREL_ITEMS=registerBarrelItems(); public static final DeferredHolder<Item,Item> BARREL=BARREL_ITEMS.get("barrel");
    public static final Map<String,DeferredHolder<Item,Item>> LOOM_ITEMS=registerLoomItems(); public static final DeferredHolder<Item,Item> LOOM=LOOM_ITEMS.get("loom");
    private static Map<String,DeferredHolder<Item,Item>> registerBarrelItems(){Map<String,DeferredHolder<Item,Item>> items=new LinkedHashMap<>();ModBlocks.BARRELS.forEach((name,block)->items.put(name,ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()))));return items;}
    private static Map<String,DeferredHolder<Item,Item>> registerLoomItems(){Map<String,DeferredHolder<Item,Item>> items=new LinkedHashMap<>();ModBlocks.LOOMS.forEach((name,block)->items.put(name,ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()))));return items;}
    private static DeferredHolder<Item,Item> blockItem(String name,DeferredHolder<Block,? extends Block> block){return ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));}
    private static DeferredHolder<Item,Item> simple(String name){return ITEMS.register(name,()->new Item(new Item.Properties()));}
    public static void register(IEventBus bus){ITEMS.register(bus);} private ModItems(){}
}
