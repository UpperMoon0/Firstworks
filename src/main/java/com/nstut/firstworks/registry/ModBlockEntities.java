package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
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
                    ModBlocks.BARRELS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(ModBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BARREL.get(), (barrel, side) -> barrel.getTank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BARREL.get(), (barrel, side) -> barrel.getItemHandler(side));
    }

    private ModBlockEntities() {}
}
