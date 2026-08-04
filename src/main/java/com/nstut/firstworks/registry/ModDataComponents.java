package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Firstworks.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DyeColor>> FLEECE_COLOR =
            COMPONENTS.register("fleece_color", () -> DataComponentType.<DyeColor>builder()
                    .persistent(DyeColor.CODEC)
                    .networkSynchronized(DyeColor.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WOOD_TYPE =
            COMPONENTS.register("wood_type", () -> DataComponentType.<String>builder()
                    .persistent(com.mojang.serialization.Codec.STRING)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8)
                    .build());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
