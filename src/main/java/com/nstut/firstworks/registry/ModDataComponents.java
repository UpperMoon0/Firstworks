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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.nstut.firstworks.content.workshop.ItemHeat>> HEAT =
            COMPONENTS.register("heat", () -> DataComponentType.<com.nstut.firstworks.content.workshop.ItemHeat>builder()
                    .persistent(com.nstut.firstworks.content.workshop.ItemHeat.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.nstut.firstworks.content.workshop.ItemHeat.CODEC)).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.nstut.firstworks.content.workshop.ForgeProgress>> FORGE_PROGRESS =
            COMPONENTS.register("forge_progress", () -> DataComponentType.<com.nstut.firstworks.content.workshop.ForgeProgress>builder()
                    .persistent(com.nstut.firstworks.content.workshop.ForgeProgress.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.nstut.firstworks.content.workshop.ForgeProgress.CODEC)).build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.nstut.firstworks.content.workshop.CoolingTreatmentState>> COOLING_TREATMENT =
            COMPONENTS.register("cooling_treatment", () -> DataComponentType.<com.nstut.firstworks.content.workshop.CoolingTreatmentState>builder()
                    .persistent(com.nstut.firstworks.content.workshop.CoolingTreatmentState.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.nstut.firstworks.content.workshop.CoolingTreatmentState.CODEC)).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> THERMAL_MARKER =
            COMPONENTS.register("thermal_marker", () -> DataComponentType.<String>builder().persistent(com.mojang.serialization.Codec.STRING)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BLOCK_REHEATING =
            COMPONENTS.register("block_reheating", () -> DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {}
}
