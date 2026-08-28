package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
import com.nstut.firstworks.content.basket.BasketBlockEntity;
import com.nstut.firstworks.content.charcoal.CharcoalPitBlockEntity;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
import com.nstut.firstworks.content.loom.LoomBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Firstworks.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL = TYPES.register(
            "barrel", () -> BlockEntityType.Builder.of(BarrelBlockEntity::new,
                    BuiltInRegistries.BLOCK.stream().filter(BarrelBlock.class::isInstance).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoomBlockEntity>> LOOM = TYPES.register(
            "loom", () -> BlockEntityType.Builder.of(LoomBlockEntity::new,
                    BuiltInRegistries.BLOCK.stream().filter(LoomBlock.class::isInstance).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrickMoldBlockEntity>> BRICK_MOLD = TYPES.register(
            "brick_mold", () -> BlockEntityType.Builder.of(BrickMoldBlockEntity::new, ModBlocks.BRICK_MOLD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasketBlockEntity>> BASKET = TYPES.register(
            "basket", () -> BlockEntityType.Builder.of(BasketBlockEntity::new, ModBlocks.BASKET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CharcoalPitBlockEntity>> CHARCOAL_PIT = TYPES.register(
            "charcoal_pit", () -> BlockEntityType.Builder.of(CharcoalPitBlockEntity::new, ModBlocks.CHARCOAL_PIT.get()).build(null));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(ModBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BARREL.get(), (barrel, side) -> barrel.getFluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BARREL.get(), (barrel, side) -> barrel.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LOOM.get(), (loom, side) -> loom.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BRICK_MOLD.get(), (mold, side) -> mold.getItemHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BASKET.get(), (basket, side) -> basket.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CHARCOAL_PIT.get(), (pit, side) -> pit.getItemHandler());
    }

    private ModBlockEntities() {}
}
