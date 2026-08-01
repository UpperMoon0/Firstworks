package com.nstut.firstworks.registry;

import com.nstut.firstworks.Firstworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Consumer;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Firstworks.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Firstworks.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> TANNIN_SOLUTION_TYPE = FLUID_TYPES.register("tannin_solution",
            () -> new FluidType(FluidType.Properties.create().density(1050).viscosity(1100)
                    .descriptionId("fluid.firstworks.tannin_solution")) {
                @Override
                @OnlyIn(Dist.CLIENT)
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
                        private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");
                        @Override public ResourceLocation getStillTexture() { return STILL; }
                        @Override public ResourceLocation getFlowingTexture() { return FLOWING; }
                        @Override public int getTintColor() { return 0xFF6B3F22; }
                    });
                }
            });

    public static final DeferredHolder<Fluid, Fluid> TANNIN_SOLUTION = FLUIDS.register("tannin_solution",
            () -> new Fluid() {
                @Override public FluidType getFluidType() { return TANNIN_SOLUTION_TYPE.get(); }
                @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) { return Shapes.block(); }
                @Override public float getOwnHeight(FluidState state) { return 1.0F; }
                @Override public float getHeight(FluidState state, BlockGetter level, BlockPos pos) { return 1.0F; }
                @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) { return false; }
                @Override protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) { return Vec3.ZERO; }
                @Override public int getTickDelay(LevelReader level) { return 5; }
                @Override protected float getExplosionResistance() { return 100.0F; }
                @Override protected BlockState createLegacyBlock(FluidState state) { return Blocks.AIR.defaultBlockState(); }
                @Override public boolean isSource(FluidState state) { return true; }
                @Override public int getAmount(FluidState state) { return 8; }
                @Override public Item getBucket() { return ModItems.TANNIN_SOLUTION_BUCKET.get(); }
            });

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private ModFluids() {}
}
