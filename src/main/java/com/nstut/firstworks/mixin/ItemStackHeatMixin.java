package com.nstut.firstworks.mixin;

import com.nstut.firstworks.content.workshop.ItemHeat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackHeatMixin {
    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"))
    private static void firstworks$normalizeColdHeat(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> callback) {
        ItemHeat.normalizeForComparison(first, second);
    }
}
