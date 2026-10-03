package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HeatTooltip {
    public static Component line(ItemStack stack, Level level, TemperatureUnit unit) {
        float fraction = ItemHeat.fraction(stack, level);
        String temperature = unit.format(HeatTemperature.celsius(fraction, HeatTemperature.maximum(stack)));
        var status = Component.translatable("heat.firstworks.status." + ItemHeat.state(fraction));
        var line = Component.translatable("heat.firstworks.tooltip", temperature, status);
        var heat = stack.get(ModDataComponents.HEAT.get());
        if (fraction >= 0.25F && heat != null) line.append(Component.translatable("heat.firstworks.window",
                HeatTemperature.workableSeconds(ItemHeat.remaining(stack, level), heat.capacity())));
        return line.withStyle(fraction >= 0.25F ? ChatFormatting.GOLD
                : fraction > 0 ? ChatFormatting.RED : ChatFormatting.GRAY);
    }
    private HeatTooltip() {}
}
