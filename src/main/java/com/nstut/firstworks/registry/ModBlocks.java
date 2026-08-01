package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Firstworks.MOD_ID);
    public static final Map<String, DeferredHolder<Block, BarrelBlock>> BARRELS = new LinkedHashMap<>();

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

    private static DeferredHolder<Block, BarrelBlock> barrel(String name) {
        DeferredHolder<Block, BarrelBlock> holder = BLOCKS.register(name,
                () -> new BarrelBlock(Block.Properties.ofFullCopy(Blocks.BARREL).noOcclusion()));
        BARRELS.put(name, holder);
        return holder;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {}
}
