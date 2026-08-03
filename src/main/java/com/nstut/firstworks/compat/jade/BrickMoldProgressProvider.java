package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.brick_mold.BrickMoldBlockEntity;
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

public enum BrickMoldProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("brick_mold_progress");
    private static final String INPUT_COUNT = "FirstworksMoldInputCount";
    private static final String REQUIRED_COUNT = "FirstworksMoldRequiredCount";
    private static final String PRESS_PROGRESS = "FirstworksMoldPressProgress";
    private static final String REQUIRED_PRESSES = "FirstworksMoldRequiredPresses";
    private static final String RESULT = "FirstworksMoldResult";
    private static final String OUTPUT = "FirstworksMoldOutput";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof BrickMoldBlockEntity mold)) return;
        data.putInt(INPUT_COUNT, mold.getInput().getCount());
        data.putInt(REQUIRED_COUNT, mold.getRequiredInputCount());
        data.putInt(PRESS_PROGRESS, mold.getPressProgress());
        mold.getMatchingRecipe().ifPresent(holder -> {
            data.putInt(REQUIRED_PRESSES, Math.max(1, holder.value().presses()));
            data.putString(RESULT, holder.value().result().getDescriptionId());
        });
        ItemStack output = mold.getOutput();
        if (!output.isEmpty()) {
            data.putString(OUTPUT, output.getDescriptionId());
            data.putInt("OutputCount", output.getCount());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(OUTPUT)) {
            tooltip.add(Component.translatable("jade.firstworks.brick_mold.ready",
                    data.getInt("OutputCount"),
                    Component.translatable(data.getString(OUTPUT)).withStyle(ChatFormatting.GOLD)));
            return;
        }
        int loaded = data.getInt(INPUT_COUNT);
        if (loaded == 0) {
            tooltip.add(Component.translatable("jade.firstworks.brick_mold.empty"));
            return;
        }
        int required = Math.max(1, data.getInt(REQUIRED_COUNT));
        if (loaded < required || !data.contains(RESULT)) {
            tooltip.add(Component.translatable("jade.firstworks.brick_mold.filling", loaded, required));
            return;
        }
        int progress = data.getInt(PRESS_PROGRESS);
        int presses = Math.max(1, data.getInt(REQUIRED_PRESSES));
        tooltip.add(Component.translatable("jade.firstworks.brick_mold.molding",
                Component.translatable(data.getString(RESULT)).withStyle(ChatFormatting.GOLD)));
        tooltip.add(IElementHelper.get().progress(
                Mth.clamp((float) progress / presses, 0.0F, 1.0F),
                Component.translatable("jade.firstworks.brick_mold.progress", progress, presses)
                        .withStyle(ChatFormatting.WHITE),
                IElementHelper.get().progressStyle().color(0xFF302B27, 0xFF51463C).textColor(0xFFFFFFFF),
                BoxStyle.getTransparent(), false).size(new Vec2(140, 12)));
    }

    @Override public ResourceLocation getUid() { return UID; }
}
