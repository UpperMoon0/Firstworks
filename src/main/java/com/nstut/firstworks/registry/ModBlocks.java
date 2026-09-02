package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.BellowsBlock;
import com.nstut.firstworks.content.ResinScarBlock;
import com.nstut.firstworks.content.StandingTorchBlock;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.basket.BasketBlock;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlock;
import com.nstut.firstworks.content.charcoal.CharcoalPileBlock;
import com.nstut.firstworks.content.loom.LoomBlock;
import com.nstut.firstworks.content.mortar.MortarBlock;
import com.nstut.firstworks.content.quern.QuernBlock;
import com.nstut.firstworks.content.workshop.CrucibleFurnaceBlock;
import com.nstut.firstworks.content.workshop.KilnBlock;
import com.nstut.firstworks.content.workshop.PotteryWheelBlock;
import com.nstut.firstworks.content.workshop.StoneAnvilBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, Firstworks.MOD_ID);
    public static final Map<String, DeferredHolder<Block, BarrelBlock>> BARRELS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, LoomBlock>> LOOMS = new LinkedHashMap<>();

    public static final DeferredHolder<Block, StandingTorchBlock> STANDING_TORCH = BLOCKS.register(
            "standing_torch",
            () -> new StandingTorchBlock(Block.Properties.of()
                    .instabreak()
                    .lightLevel(state -> 14)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<Block, BrickMoldBlock> BRICK_MOLD = BLOCKS.register(
            "brick_mold",
            () -> new BrickMoldBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredHolder<Block, BasketBlock> BASKET = BLOCKS.register(
            "basket",
            () -> new BasketBlock(Block.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS).noOcclusion()));
    public static final DeferredHolder<Block, MortarBlock> MORTAR_AND_PESTLE = BLOCKS.register(
            "mortar_and_pestle",
            () -> new MortarBlock(Block.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));
    public static final DeferredHolder<Block, CharcoalPileBlock> CHARCOAL_PILE = BLOCKS.register(
            "charcoal_pile",
            () -> new CharcoalPileBlock(Block.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredHolder<Block, QuernBlock> QUERN = BLOCKS.register(
            "quern",
            () -> new QuernBlock(Block.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));
    public static final DeferredHolder<Block, LiquidBlock> TANNIN_SOLUTION = BLOCKS.register(
            "tannin_solution",
            () -> new LiquidBlock(ModFluids.TANNIN_SOLUTION.get(), Block.Properties.of()
                    .mapColor(MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

    public static final DeferredHolder<Block, ResinScarBlock> RESIN_SCAR = BLOCKS.register(
            "resin_scar",
            () -> new ResinScarBlock(Block.Properties.of()
                    .instabreak()
                    .sound(SoundType.HONEY_BLOCK)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()));
    public static final DeferredHolder<Block, PotteryWheelBlock> POTTERY_WHEEL = BLOCKS.register(
            "pottery_wheel",
            () -> new PotteryWheelBlock(Block.Properties.ofFullCopy(Blocks.SMOOTH_STONE).noOcclusion()));
    public static final DeferredHolder<Block, KilnBlock> KILN = BLOCKS.register(
            "kiln",
            () -> new KilnBlock(Block.Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredHolder<Block, StoneAnvilBlock> STONE_ANVIL = BLOCKS.register(
            "stone_anvil",
            () -> new StoneAnvilBlock(Block.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));
    public static final DeferredHolder<Block, BellowsBlock> BELLOWS = BLOCKS.register(
            "bellows",
            () -> new BellowsBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredHolder<Block, CrucibleFurnaceBlock> CRUCIBLE_FURNACE = BLOCKS.register(
            "crucible_furnace",
            () -> new CrucibleFurnaceBlock(Block.Properties.ofFullCopy(Blocks.BRICKS)));
    public static final DeferredHolder<Block, LoomBlock> COPPER_LOOM = BLOCKS.register(
            "copper_loom",
            () -> new LoomBlock(Block.Properties.ofFullCopy(Blocks.LOOM).noOcclusion()));
    public static final DeferredHolder<Block, QuernBlock> ROTARY_QUERN = BLOCKS.register(
            "rotary_quern",
            () -> new QuernBlock(Block.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));
    public static final DeferredHolder<Block, Block> PLASTER_BLOCK = BLOCKS.register(
            "plaster_block",
            () -> new Block(Block.Properties.ofFullCopy(Blocks.CALCITE)));

    public static final DeferredHolder<Block, BarrelBlock> BARREL = barrel("barrel");
    public static final DeferredHolder<Block, BarrelBlock> SPRUCE_BARREL = barrel("spruce_barrel");
    public static final DeferredHolder<Block, BarrelBlock> BIRCH_BARREL = barrel("birch_barrel");
    public static final DeferredHolder<Block, BarrelBlock> JUNGLE_BARREL = barrel("jungle_barrel");
    public static final DeferredHolder<Block, BarrelBlock> ACACIA_BARREL = barrel("acacia_barrel");
    public static final DeferredHolder<Block, BarrelBlock> DARK_OAK_BARREL = barrel("dark_oak_barrel");
    public static final DeferredHolder<Block, BarrelBlock> MANGROVE_BARREL = barrel("mangrove_barrel");
    public static final DeferredHolder<Block, BarrelBlock> CHERRY_BARREL = barrel("cherry_barrel");
    public static final DeferredHolder<Block, BarrelBlock> BAMBOO_BARREL = barrel("bamboo_barrel");
    public static final DeferredHolder<Block, BarrelBlock> CRIMSON_BARREL = barrel("crimson_barrel");
    public static final DeferredHolder<Block, BarrelBlock> WARPED_BARREL = barrel("warped_barrel");

    public static final DeferredHolder<Block, LoomBlock> LOOM = loom("loom");
    public static final DeferredHolder<Block, LoomBlock> SPRUCE_LOOM = loom("spruce_loom");
    public static final DeferredHolder<Block, LoomBlock> BIRCH_LOOM = loom("birch_loom");
    public static final DeferredHolder<Block, LoomBlock> JUNGLE_LOOM = loom("jungle_loom");
    public static final DeferredHolder<Block, LoomBlock> ACACIA_LOOM = loom("acacia_loom");
    public static final DeferredHolder<Block, LoomBlock> DARK_OAK_LOOM = loom("dark_oak_loom");
    public static final DeferredHolder<Block, LoomBlock> MANGROVE_LOOM = loom("mangrove_loom");
    public static final DeferredHolder<Block, LoomBlock> CHERRY_LOOM = loom("cherry_loom");
    public static final DeferredHolder<Block, LoomBlock> BAMBOO_LOOM = loom("bamboo_loom");
    public static final DeferredHolder<Block, LoomBlock> CRIMSON_LOOM = loom("crimson_loom");
    public static final DeferredHolder<Block, LoomBlock> WARPED_LOOM = loom("warped_loom");

    private static DeferredHolder<Block, BarrelBlock> barrel(String name) {
        DeferredHolder<Block, BarrelBlock> holder = BLOCKS.register(name,
                () -> new BarrelBlock(Block.Properties.ofFullCopy(Blocks.BARREL).noOcclusion()));
        BARRELS.put(name, holder);
        return holder;
    }

    private static DeferredHolder<Block, LoomBlock> loom(String name) {
        DeferredHolder<Block, LoomBlock> holder = BLOCKS.register(name,
                () -> new LoomBlock(Block.Properties.ofFullCopy(Blocks.LOOM).noOcclusion()));
        LOOMS.put(name, holder);
        return holder;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}
