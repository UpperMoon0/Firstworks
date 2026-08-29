package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.quern.QuernBlock;
import com.nstut.firstworks.content.quern.QuernBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum QuernProgressProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("quern_progress");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof QuernBlockEntity quern)) {
            return;
        }

        ItemStack input = quern.getInput();
        ItemStack output = quern.getOutput();
        if (!input.isEmpty()) {
            data.putString("In", input.getDescriptionId());
            data.putInt("Count", input.getCount());
        }
        if (!output.isEmpty()) {
            data.putString("Out", output.getDescriptionId());
            data.putInt("OutCount", output.getCount());
        }
        data.putInt("Done", quern.getProgress());
        data.putInt("Need", quern.requiredWork());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains("Out")) {
            tooltip.add(Component.translatable("jade.firstworks.quern.ready", data.getInt("OutCount"),
                    Component.translatable(data.getString("Out")).withStyle(ChatFormatting.GOLD)));
            return;
        }
        if (!data.contains("In")) {
            tooltip.add(Component.translatable("jade.firstworks.quern.empty"));
            return;
        }

        tooltip.add(Component.translatable(data.getString("In")).withStyle(ChatFormatting.GOLD));
        int required = Math.max(1, data.getInt("Need"));
        tooltip.add(Component.translatable("jade.firstworks.quern.progress",
                Math.min(100, data.getInt("Done") * 100 / required)));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
