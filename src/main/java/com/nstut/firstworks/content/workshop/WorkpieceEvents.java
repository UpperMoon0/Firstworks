package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.Firstworks;
import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Firstworks.MOD_ID)
public final class WorkpieceEvents {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var player = event.getEntity();
        if (player == null) return;
        var stack = event.getItemStack();
        if (stack.has(ModDataComponents.HEAT.get()) || ItemHeat.capacity(stack, player.level()) > 0) {
            float heat = ItemHeat.fraction(stack, player.level());
            event.getToolTip().add(Component.translatable("heat.firstworks." + ItemHeat.state(heat))
                    .withStyle(heat >= 0.25F ? ChatFormatting.GOLD : heat > 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
        var progress = stack.get(ModDataComponents.FORGE_PROGRESS.get());
        if (progress != null) event.getToolTip().add(Component.translatable("jade.firstworks.workshop.progress",
                progress.completed(), progress.sequence().split(",").length).withStyle(ChatFormatting.GRAY));
    }
    @SubscribeEvent public static void coolInventory(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.has(ModDataComponents.HEAT.get()) && ItemHeat.remaining(stack, player.level()) == 0)
                stack.remove(ModDataComponents.HEAT.get());
        }
    }
    private WorkpieceEvents() {}
}
