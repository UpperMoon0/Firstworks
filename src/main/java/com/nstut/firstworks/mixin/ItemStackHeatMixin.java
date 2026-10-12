package com.nstut.firstworks.mixin;

import com.nstut.firstworks.content.workshop.ItemHeat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackHeatMixin {
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void firstworks$quenchDuration(net.minecraft.world.entity.LivingEntity entity, CallbackInfoReturnable<Integer> callback) {
        if (com.nstut.firstworks.content.workshop.CoolingEvents.isQuenching(entity)) callback.setReturnValue(72000);
    }

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"))
    private static void firstworks$normalizeColdHeat(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> callback) {
        ItemHeat.normalizeForComparison(first, second);
    }
}
