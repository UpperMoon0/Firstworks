package com.nstut.firstworks.content;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class OchreTooltip {
    @SubscribeEvent
    public static void addPigmentInstructions(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.RAW_OCHRE.get())) {
            event.getToolTip().add(Component.translatable("tooltip.firstworks.raw_ochre").withStyle(ChatFormatting.GRAY));
        } else if (event.getItemStack().is(ModItems.GROUND_OCHRE.get())) {
            event.getToolTip().add(Component.translatable("tooltip.firstworks.ground_ochre").withStyle(ChatFormatting.GRAY));
        }
    }
    private OchreTooltip() {}
}
