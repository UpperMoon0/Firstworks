package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES,
            Firstworks.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID,
            Firstworks.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> TANNIN_SOLUTION_TYPE = FLUID_TYPES.register("tannin_solution",
            () -> new FluidType(FluidType.Properties.create()
                    .density(1000)
                    .viscosity(1000)
                    .canExtinguish(true)
                    .canConvertToSource(true)
                    .supportsBoating(true)
                    .canHydrate(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .descriptionId("fluid.firstworks.tannin_solution")) {
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
                        private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");
                        private static final ResourceLocation OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

                        @Override public ResourceLocation getStillTexture() { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOWING; }
                        @Override public ResourceLocation getOverlayTexture() { return OVERLAY; }
                        @Override public int getTintColor() { return 0xFF6B3F22; }
                    });
                }
            });

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TANNIN_SOLUTION = FLUIDS.register(
            "tannin_solution", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_TANNIN_SOLUTION = FLUIDS.register(
            "flowing_tannin_solution", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(TANNIN_SOLUTION_TYPE, TANNIN_SOLUTION, FLOWING_TANNIN_SOLUTION)
                .bucket(ModItems.TANNIN_SOLUTION_BUCKET)
                .block(ModBlocks.TANNIN_SOLUTION)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .explosionResistance(100.0F);
    }

    public static void register(net.neoforged.bus.api.IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private ModFluids() {}
}
