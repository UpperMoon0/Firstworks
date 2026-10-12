package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.WeakHashMap;

public final class HeatProtection {
    private static final Map<Player,Long> HINTS = new WeakHashMap<>();
    public static boolean protectedByEquipment(Player player,InteractionHand hand) {
        if (player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).is(ModTags.HEAT_PROTECTION)) return true;
        for (ItemStack armor : player.getArmorSlots()) if (armor.is(ModTags.HEAT_PROTECTION)) return true;
        return false;
    }
    public static boolean allow(Player player,ItemStack stack,InteractionHand hand) {
        if (!FirstworksConfig.REQUIRE_HEAT_PROTECTION.get() || player.hasInfiniteMaterials()
                || ItemHeat.celsius(stack,player.level()) < FirstworksConfig.HOT_HANDLING_THRESHOLD.get()
                || protectedByEquipment(player,hand)) return true;
        long now=player.level().getGameTime();
        if (!player.level().isClientSide && now-HINTS.getOrDefault(player,now-40)>=40) {
            player.displayClientMessage(Component.translatable("hint.firstworks.heat_protection"),true);
            HINTS.put(player,now);
        }
        return false;
    }
    private HeatProtection() {}
}
