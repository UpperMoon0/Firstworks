package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.workshop.WorkshopBlockEntity;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import com.nstut.firstworks.content.workshop.ItemHeat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
    private static final String ACTIONS = "FirstworksForgeActions";
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
        var visible = workshop.getOutput().isEmpty() ? workshop.getInput() : workshop.getOutput();
        data.putFloat("Heat", ItemHeat.fraction(visible, workshop.getLevel()));
        data.putBoolean("Heatable", ItemHeat.capacity(visible, workshop.getLevel()) > 0
                || visible.has(com.nstut.firstworks.registry.ModDataComponents.HEAT.get()));
        data.putString("HeatState", ItemHeat.state(visible, workshop.getLevel()));
        data.putInt("BurnTicks", workshop.getBurnTicks());
        data.putDouble("Temperature", workshop.getTemperature());
        data.putDouble("MaxTemperature", workshop.getMaxTemperature());
        data.putBoolean("NeedsIgnition", workshop.needsIgnition());
        putStack(data, INPUT, INPUT_COUNT, workshop.getInput());
        putStack(data, CATALYST, CATALYST_COUNT, workshop.getCatalyst());
        data.putInt(FUEL_COUNT, workshop.getFuel().getCount());
        putStack(data, OUTPUT, OUTPUT_COUNT, workshop.getOutput());
        data.putInt(PROGRESS, workshop.getProgress());
        data.putInt(STOKE, workshop.getStokeTicks());
        data.putBoolean(RUNNING, workshop.isRunning());
        if (WorkshopRecipe.STONE_ANVIL.equals(workshop.station()))
            data.putInt("RecipeChoices", workshop.getMatchingRecipeCount());
        workshop.activeRecipe().ifPresent(holder -> {
            data.putString(RESULT, holder.value().result().getDescriptionId());
            data.putInt("RequiredTemperature", holder.value().requiredTemperature());
            data.putInt(WORK, holder.value().requiredWork());
            holder.value().forge().ifPresent(forge -> {
                ListTag actions = new ListTag();
                forge.actions().forEach(action -> actions.add(StringTag.valueOf(action)));
                data.put(ACTIONS, actions);
            });
        });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        String station = data.getString(STATION);
        if (WorkshopRecipe.STONE_ANVIL.equals(station)) {
            String action = com.nstut.firstworks.content.workshop.StoneAnvilBlock.actionAt(
                    accessor.getBlockState(), accessor.getPosition(), accessor.getHitResult());
            tooltip.add(Component.translatable("jade.firstworks.anvil.target",
                    Component.translatable("action.firstworks." + action)).withStyle(ChatFormatting.YELLOW));
        }
        var unit = com.nstut.firstworks.FirstworksClientConfig.HEAT_UNIT.get();
        if (WorkshopRecipe.KILN.equals(station) || WorkshopRecipe.CRUCIBLE_FURNACE.equals(station))
            tooltip.add(Component.translatable("jade.firstworks.workshop.temperature",
                    unit.format(data.getDouble("Temperature")), unit.format(data.getDouble("MaxTemperature"))).withStyle(ChatFormatting.GOLD));
        if (data.getInt("BurnTicks") > 0) tooltip.add(Component.translatable("jade.firstworks.workshop.burn_time",
                (data.getInt("BurnTicks") + 19) / 20).withStyle(ChatFormatting.GOLD));
        if (data.getBoolean("NeedsIgnition") && !WorkshopRecipe.KILN.equals(station)) tooltip.add(Component.translatable("hint.firstworks.workshop.ignite")
                .withStyle(ChatFormatting.YELLOW));
        if (data.getBoolean("Heatable")) {
            float heat = data.getFloat("Heat");
            tooltip.add(Component.translatable("heat.firstworks." + data.getString("HeatState"))
                    .withStyle("workable".equals(data.getString("HeatState")) ? ChatFormatting.GOLD : heat > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
        if (data.contains(OUTPUT)) {
            tooltip.add(Component.translatable("hint.firstworks.collect"));
            return;
        }

        if (!data.contains(INPUT)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.empty"));
            appendManualHint(tooltip, station);
            return;
        }

        if (WorkshopRecipe.KILN.equals(station)) {
            tooltip.add(Component.translatable(data.getInt("BurnTicks") > 0
                    ? "jade.firstworks.kiln.heating" : data.getBoolean("NeedsIgnition")
                            ? "hint.firstworks.workshop.ignite" : "jade.firstworks.workshop.needs_fuel").withStyle(ChatFormatting.GRAY));
            return;
        }

        if (!data.contains(RESULT)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.incomplete")
                    .withStyle(ChatFormatting.YELLOW));
            appendManualHint(tooltip, station);
            return;
        }

        if (WorkshopRecipe.STONE_ANVIL.equals(station)) {
            var result = Component.translatable(data.getString(RESULT)).withStyle(ChatFormatting.GOLD);
            tooltip.add(data.getInt("RecipeChoices") > 1
                    ? Component.translatable("jade.firstworks.anvil.auto_selected", result, data.getInt("RecipeChoices"))
                    : Component.translatable("jade.firstworks.anvil.recipe", result));
        } else tooltip.add(Component.translatable("jade.firstworks.workshop.making",
                Component.translatable(data.getString(RESULT)).withStyle(ChatFormatting.GOLD)));
        int progress = data.getInt(PROGRESS);
        if (WorkshopRecipe.STONE_ANVIL.equals(station) && data.contains(ACTIONS)) {
            ListTag actions = data.getList(ACTIONS, Tag.TAG_STRING);
            for (int start = 0; start < actions.size(); start += 4) {
                var sequence = Component.empty();
                for (int i = start; i < Math.min(start + 4, actions.size()); i++) {
                    if (i > start) sequence.append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY));
                    var action = Component.translatable("action.firstworks." + actions.getString(i));
                    if (i < progress) {
                        sequence.append(action.withStyle(ChatFormatting.GREEN));
                    } else if (i == progress) {
                        sequence.append(Component.literal("[").append(action).append("]")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    } else {
                        sequence.append(action.withStyle(ChatFormatting.GRAY));
                    }
                }
                tooltip.add(sequence);
            }
            return;
        }
        int work = Math.max(1, data.getInt(WORK));
        tooltip.add(IElementHelper.get().progress(
                Mth.clamp((float) progress / work, 0.0F, 1.0F),
                Component.translatable("jade.firstworks.workshop.progress", progress, work)
                        .withStyle(ChatFormatting.WHITE),
                IElementHelper.get().progressStyle().color(0xFF302B27, 0xFF51463C).textColor(0xFFFFFFFF),
                BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));

        if (WorkshopRecipe.CRUCIBLE_FURNACE.equals(station)) {
            int fuel = data.getInt(FUEL_COUNT);
            if (fuel == 0 && data.getInt("BurnTicks") == 0 && !data.getBoolean(RUNNING) && progress == 0) {
                tooltip.add(Component.translatable("jade.firstworks.workshop.needs_fuel")
                        .withStyle(ChatFormatting.YELLOW));
            }

            if (data.getDouble("Temperature") < data.getInt("RequiredTemperature"))
                tooltip.add(Component.translatable("jade.firstworks.workshop.required_temperature",
                        unit.format(data.getInt("RequiredTemperature"))).withStyle(ChatFormatting.YELLOW));
        } else {
            appendManualHint(tooltip, station);
        }
    }

    private static void appendManualHint(ITooltip tooltip, String station) {
        if (WorkshopRecipe.POTTERY_WHEEL.equals(station)) {
            tooltip.add(Component.translatable("jade.firstworks.workshop.pottery_action")
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
