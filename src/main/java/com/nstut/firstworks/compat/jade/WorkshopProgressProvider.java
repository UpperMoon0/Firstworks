package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum WorkshopProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("workshop_progress");
    private static final String STATION = "FirstworksWorkshopStation";
    private static final String INPUT = "FirstworksWorkshopInput";
    private static final String INPUT_COUNT = "FirstworksWorkshopInputCount";
    private static final String CATALYST = "FirstworksWorkshopCatalyst";
    private static final String CATALYST_COUNT = "FirstworksWorkshopCatalystCount";
    private static final String FUEL_COUNT = "FirstworksWorkshopFuelCount";
    private static final String OUTPUT = "FirstworksWorkshopOutput";
    private static final String OUTPUT_COUNT = "FirstworksWorkshopOutputCount";
    private static final String RESULT = "FirstworksWorkshopResult";
    private static final String PROGRESS = "FirstworksWorkshopProgress";
    private static final String WORK = "FirstworksWorkshopWork";
    private static final String STOKE = "FirstworksWorkshopStoke";
    private static final String RUNNING = "FirstworksWorkshopRunning";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof WorkshopBlockEntity workshop)) {
            return;
        }

        data.putString(STATION, workshop.station());
        putStack(data, INPUT, INPUT_COUNT, workshop.getInput());
        putStack(data, CATALYST, CATALYST_COUNT, workshop.getCatalyst());
        data.putInt(FUEL_COUNT, workshop.getFuel().getCount());
        putStack(data, OUTPUT, OUTPUT_COUNT, workshop.getOutput());
        data.putInt(PROGRESS, workshop.getProgress());
        data.putInt(STOKE, workshop.getStokeTicks());
        data.putBoolean(RUNNING, workshop.isRunning());
        workshop.activeRecipe().ifPresent(holder -> {
            data.putString(RESULT, holder.value().result().getDescriptionId());
            data.putInt(WORK, Math.max(1, holder.value().work()));
        });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        String station = data.getString(STATION);

        if (data.contains(OUTPUT)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.ready", data.getInt(OUTPUT_COUNT),
                    Component.translatable(data.getString(OUTPUT)).withStyle(ChatFormatting.GOLD)));
            return;
        }

        if (!data.contains(INPUT)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.empty"));
            appendManualHint(tooltip, station);
            return;
        }

        tooltip.add(Component.translatable("jade.firstworks.workshop.input", data.getInt(INPUT_COUNT),
                Component.translatable(data.getString(INPUT)).withStyle(ChatFormatting.GOLD)));
        if (data.contains(CATALYST)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.catalyst", data.getInt(CATALYST_COUNT),
                    Component.translatable(data.getString(CATALYST)).withStyle(ChatFormatting.GOLD)));
        }

        if (!data.contains(RESULT)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.incomplete")
                    .withStyle(ChatFormatting.YELLOW));
            appendManualHint(tooltip, station);
            return;
        }

        tooltip.add(Component.translatable("jade.firstworks.workshop.making",
                Component.translatable(data.getString(RESULT)).withStyle(ChatFormatting.GOLD)));
        int progress = data.getInt(PROGRESS);
        int work = Math.max(1, data.getInt(WORK));
        tooltip.add(IElementHelper.get().progress(
                Mth.clamp((float) progress / work, 0.0F, 1.0F),
                Component.translatable("jade.firstworks.workshop.progress", progress, work)
                        .withStyle(ChatFormatting.WHITE),
                IElementHelper.get().progressStyle().color(0xFF302B27, 0xFF51463C).textColor(0xFFFFFFFF),
                BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));

        if (WorkshopRecipe.KILN.equals(station) || WorkshopRecipe.CRUCIBLE_FURNACE.equals(station)) {
            int fuel = data.getInt(FUEL_COUNT);
            if (fuel > 0) {
                tooltip.add(Component.translatable("jade.firstworks.workshop.fuel_reserve", fuel));
            } else if (!data.getBoolean(RUNNING) && progress == 0) {
                tooltip.add(Component.translatable("jade.firstworks.workshop.needs_fuel")
                        .withStyle(ChatFormatting.YELLOW));
            }

            if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(station)) {
                int airTicks = data.getInt(STOKE);
                if (airTicks <= 0) {
                    tooltip.add(Component.translatable("jade.firstworks.workshop.needs_air")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.translatable("jade.firstworks.workshop.air_reserve",
                            (airTicks + 19) / 20));
                }
            }
        } else {
            appendManualHint(tooltip, station);
        }
    }

    private static void appendManualHint(ITooltip tooltip, String station) {
        if (WorkshopRecipe.POTTERY_WHEEL.equals(station)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.pottery_action")
                    .withStyle(ChatFormatting.GRAY));
        } else if (WorkshopRecipe.STONE_ANVIL.equals(station)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.anvil_action")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void putStack(CompoundTag data, String itemKey, String countKey, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        data.putString(itemKey, stack.getDescriptionId());
        data.putInt(countKey, stack.getCount());
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
