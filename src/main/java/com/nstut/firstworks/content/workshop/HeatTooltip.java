package com.nstut.firstworks.content.workshop;

import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class HeatTooltip {
    public static Component line(ItemStack stack, Level level, TemperatureUnit unit) {
        float fraction = ItemHeat.fraction(stack, level);
        String temperature = unit.format(ItemHeat.celsius(stack, level));
        var status = Component.translatable("heat.firstworks.status." + ItemHeat.state(stack, level));
        var line = Component.translatable("heat.firstworks.tooltip", temperature, status);
        var heat = stack.get(ModDataComponents.HEAT.get());
        if (ItemHeat.workable(stack, level) && heat != null) line.append(Component.translatable("heat.firstworks.window",
                ThermalModel.workableSeconds(ItemHeat.celsius(stack, level), ItemHeat.minimumWorkable(stack, level), ItemHeat.coolingRate(stack))));
        return line.withStyle(ItemHeat.workable(stack, level) ? ChatFormatting.GOLD
                : fraction > 0 ? ChatFormatting.RED : ChatFormatting.GRAY);
    }
    private HeatTooltip() {}
}
