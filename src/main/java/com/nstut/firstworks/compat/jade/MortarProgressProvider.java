package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
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

public enum MortarProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("mortar_progress");
    private static final String INPUT_ITEM = "FirstworksMortarInputItem";
    private static final String INPUT_COUNT = "FirstworksMortarInputCount";
    private static final String REQUIRED_COUNT = "FirstworksMortarRequiredCount";
    private static final String IS_GRINDING = "FirstworksMortarGrinding";
    private static final String REMAINING_TICKS = "FirstworksMortarRemainingTicks";
    private static final String DURATION = "FirstworksMortarDuration";
    private static final String RESULT_ITEM = "FirstworksMortarResultItem";
    private static final String RESULT_COUNT = "FirstworksMortarResultCount";
    private static final String OUTPUT_ITEM = "FirstworksMortarOutputItem";
    private static final String OUTPUT_COUNT = "FirstworksMortarOutputCount";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MortarBlockEntity mortar)) return;
        ItemStack output = mortar.getOutput();
        if (!output.isEmpty()) {
            data.putString(OUTPUT_ITEM, output.getDescriptionId());
            data.putInt(OUTPUT_COUNT, output.getCount());
            return;
        }
        ItemStack input = mortar.getInput();
        if (!input.isEmpty()) {
            data.putString(INPUT_ITEM, input.getDescriptionId());
            data.putInt(INPUT_COUNT, input.getCount());
        }
        boolean grinding = mortar.isGrinding();
        data.putBoolean(IS_GRINDING, grinding);
        mortar.getActiveRecipe().ifPresent(holder -> {
            var recipe = holder.value();
            data.putInt(REQUIRED_COUNT, recipe.inputCount());
            data.putInt(DURATION, Math.max(1, recipe.duration()));
            data.putString(RESULT_ITEM, recipe.result().getDescriptionId());
            data.putInt(RESULT_COUNT, recipe.result().getCount());
        });
        if (grinding && accessor.getLevel() != null) {
            long remaining = Math.max(0L, mortar.getFinishGameTime() - accessor.getLevel().getGameTime());
            data.putLong(REMAINING_TICKS, remaining);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        // 1. Output ready to collect
        if (data.contains(OUTPUT_ITEM)) {
            tooltip.add(Component.translatable("jade.firstworks.mortar.ready",
                    data.getInt(OUTPUT_COUNT),
                    Component.translatable(data.getString(OUTPUT_ITEM)).withStyle(ChatFormatting.GOLD)));
            tooltip.add(Component.translatable("jade.firstworks.mortar.prompt_collect")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        // 2. Empty
        if (!data.contains(INPUT_ITEM)) {
            tooltip.add(Component.translatable("jade.firstworks.mortar.empty"));
            return;
        }

        int count = data.getInt(INPUT_COUNT);
        int required = Math.max(1, data.getInt(REQUIRED_COUNT));
        boolean grinding = data.getBoolean(IS_GRINDING);

        // 3. Grinding in progress
        if (grinding) {
            long remainingTicks = data.getLong(REMAINING_TICKS);
            int duration = Math.max(1, data.getInt(DURATION));
            float progress = Mth.clamp(1.0F - (float) remainingTicks / duration, 0.0F, 1.0F);
            float secondsLeft = (float) remainingTicks / 20.0F;
            String timeStr = "%.1f s".formatted(secondsLeft);

            tooltip.add(Component.translatable("jade.firstworks.mortar.grinding",
                    Component.translatable(data.getString(INPUT_ITEM)).withStyle(ChatFormatting.GOLD)));
            tooltip.add(IElementHelper.get().progress(
                    progress,
                    Component.translatable("jade.firstworks.mortar.remaining", timeStr)
                            .withStyle(ChatFormatting.WHITE),
                    IElementHelper.get().progressStyle().color(0xFF302B27, 0xFF51463C).textColor(0xFFFFFFFF),
                    BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));
            if (data.contains(RESULT_ITEM)) {
                tooltip.add(Component.translatable("jade.firstworks.mortar.making",
                        Component.translatable(data.getString(RESULT_ITEM)).withStyle(ChatFormatting.GOLD)));
            }
            return;
        }

        // 4. Partially loaded (not enough material)
        if (count < required) {
            tooltip.add(Component.translatable(data.getString(INPUT_ITEM)).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("jade.firstworks.mortar.material", count, required)
                    .withStyle(ChatFormatting.WHITE));
            return;
        }

        // 5. Ready to grind
        tooltip.add(Component.translatable(data.getString(INPUT_ITEM)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" × " + count).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.translatable("jade.firstworks.mortar.ready_to_grind")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("jade.firstworks.mortar.prompt_grind")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
