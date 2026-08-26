package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.barrel.BarrelBlock;
import com.nstut.firstworks.content.barrel.BarrelBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum BarrelProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("barrel_progress");
    private static final String ACTIVE = "FirstworksActive";
    private static final String PROGRESS = "FirstworksProgress";
    private static final String DURATION = "FirstworksDuration";
    private static final String CANCELLED = "FirstworksCancelled";
    private static final String SEALED = "FirstworksSealed";
    private static final String OUTPUT_ITEM = "FirstworksOutputItem";
    private static final String OUTPUT_FLUID = "FirstworksOutputFluid";
    private static final String STORED_INPUT_FLUID = "FirstworksStoredInputFluid";
    private static final String STORED_INPUT_AMOUNT = "FirstworksStoredInputAmount";
    private static final String STORED_OUTPUT_FLUID = "FirstworksStoredOutputFluid";
    private static final String STORED_OUTPUT_AMOUNT = "FirstworksStoredOutputAmount";
    private static final String TOTAL_FLUID = "FirstworksTotalFluid";
    private static final String CAPACITY = "FirstworksCapacity";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BarrelBlockEntity barrel)) return;
        int duration = barrel.getActiveDuration();
        data.putBoolean(ACTIVE, duration > 0);
        data.putInt(PROGRESS, barrel.getProgress());
        data.putInt(DURATION, duration);
        data.putBoolean(CANCELLED, barrel.isProcessCancelled());
        data.putBoolean(SEALED, accessor.getBlockState().getValue(BarrelBlock.SEALED));
        barrel.getActiveRecipe().ifPresent(holder -> {
            var recipe = holder.value();
            if (!recipe.result().isEmpty()) {
                data.putString(OUTPUT_ITEM, recipe.result().getDescriptionId());
            }
            if (!recipe.outputFluid().equals(com.nstut.firstworks.content.barrel.BarrelRecipe.NO_FLUID)) {
                var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(recipe.outputFluid());
                if (fluid != Fluids.EMPTY) {
                    data.putString(OUTPUT_FLUID, fluid.getFluidType().getDescriptionId());
                }
            }
        });
        FluidStack input = barrel.getInputTank().getFluid();
        if (!input.isEmpty()) {
            data.putString(STORED_INPUT_FLUID, input.getFluid().getFluidType().getDescriptionId());
            data.putInt(STORED_INPUT_AMOUNT, input.getAmount());
        }
        FluidStack storedOutput = barrel.getOutputTank().getFluid();
        if (!storedOutput.isEmpty()) {
            data.putString(STORED_OUTPUT_FLUID, storedOutput.getFluid().getFluidType().getDescriptionId());
            data.putInt(STORED_OUTPUT_AMOUNT, storedOutput.getAmount());
        }
        data.putInt(TOTAL_FLUID, barrel.getTotalFluidAmount());
        data.putInt(CAPACITY, com.nstut.firstworks.content.barrel.BarrelBlockEntity.CAPACITY);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.getBoolean(CANCELLED)) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.cancelled"));
            return;
        }
        if (!data.getBoolean(ACTIVE)) {
            tooltip.add(Component.translatable(data.getBoolean(SEALED)
                    ? "jade.firstworks.barrel.waiting"
                    : "jade.firstworks.barrel.open"));
            return;
        }

        int progress = data.getInt(PROGRESS);
        int duration = Math.max(1, data.getInt(DURATION));
        float fraction = Mth.clamp((float) progress / duration, 0.0F, 1.0F);
        int secondsLeft = Math.max(0, (duration - progress + 19) / 20);
        String timeLeft = secondsLeft >= 60
                ? "%d:%02d".formatted(secondsLeft / 60, secondsLeft % 60)
                : "%d s".formatted(secondsLeft);
        IElementHelper helper = IElementHelper.get();
        boolean hasItemOutput = data.contains(OUTPUT_ITEM);
        boolean hasFluidOutput = data.contains(OUTPUT_FLUID);
        if (hasItemOutput && hasFluidOutput) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.making_both",
                    Component.translatable(data.getString(OUTPUT_ITEM)).withStyle(ChatFormatting.GOLD),
                    Component.translatable(data.getString(OUTPUT_FLUID)).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.WHITE));
        } else if (hasItemOutput) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.making",
                    Component.translatable(data.getString(OUTPUT_ITEM)).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.WHITE));
        } else if (hasFluidOutput) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.making",
                    Component.translatable(data.getString(OUTPUT_FLUID)).withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(helper.progress(
                fraction,
                Component.translatable("jade.firstworks.barrel.processing", timeLeft)
                        .withStyle(ChatFormatting.WHITE),
                helper.progressStyle()
                        .color(0xFF302B27, 0xFF51463C)
                        .textColor(0xFFFFFFFF),
                BoxStyle.getTransparent(),
                false).size(new Vec2(140, 12)));
        appendStoredFluids(tooltip, data);
    }

    private void appendStoredFluids(ITooltip tooltip, CompoundTag data) {
        if (data.contains(STORED_INPUT_FLUID)) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.input_fluid",
                    Component.translatable(data.getString(STORED_INPUT_FLUID)), data.getInt(STORED_INPUT_AMOUNT))
                    .withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable("jade.firstworks.barrel.input_fluid",
                    Component.translatable("jade.firstworks.barrel.empty"), 0).withStyle(ChatFormatting.WHITE));
        }
        if (data.contains(STORED_OUTPUT_FLUID)) {
            tooltip.add(Component.translatable("jade.firstworks.barrel.output_fluid",
                    Component.translatable(data.getString(STORED_OUTPUT_FLUID)), data.getInt(STORED_OUTPUT_AMOUNT))
                    .withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.add(Component.translatable("jade.firstworks.barrel.output_fluid",
                    Component.translatable("jade.firstworks.barrel.empty"), 0).withStyle(ChatFormatting.WHITE));
        }
        tooltip.add(Component.translatable("jade.firstworks.barrel.fluid_capacity",
                data.getInt(TOTAL_FLUID), data.getInt(CAPACITY)).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
