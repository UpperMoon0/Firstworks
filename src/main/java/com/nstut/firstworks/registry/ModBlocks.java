package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.StandingTorchBlock;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlock;
import com.nstut.firstworks.content.loom.LoomBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Firstworks.MOD_ID);
    public static final Map<String, DeferredHolder<Block, BarrelBlock>> BARRELS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, LoomBlock>> LOOMS = new LinkedHashMap<>();

    public static final DeferredHolder<Block, StandingTorchBlock> STANDING_TORCH = BLOCKS.register("standing_torch",
            () -> new StandingTorchBlock(Block.Properties.of()
                    .noCollission()
                    .instabreak()
                    .lightLevel(state -> 14)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, BrickMoldBlock> BRICK_MOLD = BLOCKS.register("brick_mold",
            () -> new BrickMoldBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));

    public static final DeferredHolder<Block, LiquidBlock> TANNIN_SOLUTION = BLOCKS.register("tannin_solution",
            () -> new LiquidBlock(ModFluids.TANNIN_SOLUTION.get(), Block.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WATER)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()
                    .sound(SoundType.EMPTY)));

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
