package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.mortar.MortarBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum MortarProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MortarBlockEntity mortar)) return;
        data.putInt("Stage", mortar.getStageIndex() + 1);
        data.putInt("Progress", mortar.getStageProgress());
        data.putBoolean("Grinding", mortar.isGrinding());
        data.putBoolean("Cancelled", mortar.isProcessCancelled());
        data.putInt("Input", mortar.getInput().getCount());
        if (!mortar.getOutput().isEmpty()) data.putBoolean("Output", true);
        mortar.getActiveRecipe().ifPresent(h -> {
            data.putInt("Required", h.value().inputCount());
            data.putInt("Stages", h.value().stages().size());
        });
        mortar.getStage().ifPresent(stage -> {
            data.putString("Action", stage.action());
            data.putInt("Work", stage.work());
        });
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (data.getBoolean("Output")) { tooltip.add(Component.translatable("hint.firstworks.collect")); return; }
        if (data.getBoolean("Cancelled")) { tooltip.add(Component.translatable("hint.firstworks.cancelled")); return; }
        if (data.getInt("Input") == 0) { tooltip.add(Component.translatable("jade.firstworks.mortar.empty")); return; }
        if (!data.contains("Required")) { tooltip.add(Component.translatable("hint.firstworks.unsupported")); return; }
        if (data.getInt("Input") < data.getInt("Required")) {
            tooltip.add(Component.translatable("hint.firstworks.input_count", data.getInt("Input"), data.getInt("Required")));
            return;
        }
        tooltip.add(Component.translatable("hint.firstworks.mortar.stage", data.getInt("Stage"), data.getInt("Stages"),
                Component.translatable("action.firstworks." + data.getString("Action")), data.getInt("Progress"), data.getInt("Work")));
        tooltip.add(Component.translatable("hint.firstworks.mortar." + data.getString("Action")));
        tooltip.add(Component.translatable(data.getBoolean("Grinding") ? "hint.firstworks.mortar.active" : "hint.firstworks.mortar.paused"));
    }
    @Override public ResourceLocation getUid() { return Firstworks.id("mortar_progress"); }
}
