package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.loom.LoomBlockEntity;
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

public enum LoomProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("loom_progress");
    private static final String INPUT_COUNT = "FirstworksLoomInputCount";
    private static final String REQUIRED_COUNT = "FirstworksLoomRequiredCount";
    private static final String PROGRESS = "FirstworksLoomProgress";
    private static final String STROKES = "FirstworksLoomStrokes";
    private static final String RESULT = "FirstworksLoomResult";
    private static final String OUTPUT = "FirstworksLoomOutput";
    private static final String CANCELLED = "FirstworksLoomCancelled";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof LoomBlockEntity loom)) return;
        data.putInt(INPUT_COUNT, loom.getInput().getCount());
        data.putInt(PROGRESS, loom.getProgress());
        data.putBoolean(CANCELLED, loom.isProcessCancelled());
        ItemStack output = loom.getOutput();
        if (!output.isEmpty()) data.putString(OUTPUT, output.getDescriptionId());
        loom.getMatchingRecipe().ifPresent(holder -> {
            data.putInt(REQUIRED_COUNT, holder.value().inputCount());
            data.putInt(STROKES, Math.max(1, holder.value().strokes()));
            data.putString(RESULT, holder.value().result().getDescriptionId());
        });
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(OUTPUT)) {
            tooltip.add(Component.translatable("jade.firstworks.loom.output",
                    Component.translatable(data.getString(OUTPUT)).withStyle(ChatFormatting.GOLD)));
            return;
        }
        if (data.getBoolean(CANCELLED)) {
            tooltip.add(Component.translatable("jade.firstworks.loom.cancelled"));
            return;
        }
        int loaded = data.getInt(INPUT_COUNT);
        if (!data.contains(RESULT)) {
            tooltip.add(Component.translatable("jade.firstworks.loom.empty"));
            return;
        }
        int required = Math.max(1, data.getInt(REQUIRED_COUNT));
        if (loaded < required) {
            tooltip.add(Component.translatable("jade.firstworks.loom.loading", loaded, required));
            return;
        }
        int progress = data.getInt(PROGRESS);
        int strokes = Math.max(1, data.getInt(STROKES));
        tooltip.add(Component.translatable("jade.firstworks.loom.weaving",
                Component.translatable(data.getString(RESULT)).withStyle(ChatFormatting.GOLD)));
        tooltip.add(IElementHelper.get().progress(
                Mth.clamp((float) progress / strokes, 0.0F, 1.0F),
                Component.translatable("jade.firstworks.loom.stroke", progress, strokes)
                        .withStyle(ChatFormatting.WHITE),
                IElementHelper.get().progressStyle().color(0xFF8B5A2B, 0xFFD8C3A5).textColor(0xFFFFFFFF),
                BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));
    }

    @Override public ResourceLocation getUid() { return UID; }
}
