package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

/** Standard fire-starting ability plus a datapack fallback for modded igniters. */
public final class WorkshopIgnition {
    public static boolean isIgniter(ItemStack stack) {
        return !stack.isEmpty() && (stack.canPerformAction(ItemAbilities.FIRESTARTER_LIGHT)
                || stack.is(ModTags.WORKSTATION_IGNITERS));
    }

    public static void consume(ItemStack stack, Player player, InteractionHand hand) {
        if (player.hasInfiniteMaterials()) return;
        if (stack.isDamageableItem()) stack.hurtAndBreak(1, player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        else stack.shrink(1);
    }

    private WorkshopIgnition() {}
}
