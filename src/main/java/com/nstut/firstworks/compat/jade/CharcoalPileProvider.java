package com.nstut.firstworks.compat.jade;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.content.charcoal.CharcoalPileBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CharcoalPileProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = Firstworks.id("charcoal_pile");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof CharcoalPileBlock) {
            int amount = accessor.getBlockState().getValue(CharcoalPileBlock.AMOUNT);
            tooltip.add(Component.translatable("jade.firstworks.charcoal_pile.stored", amount)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
