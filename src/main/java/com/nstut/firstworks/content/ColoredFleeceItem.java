package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class ColoredFleeceItem extends Item {
    public ColoredFleeceItem(Properties properties) {
        super(properties);
    }

    public static DyeColor color(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLEECE_COLOR.get(), DyeColor.WHITE);
    }

    public static ItemStack create(Item item, DyeColor color, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (color != DyeColor.WHITE) stack.set(ModDataComponents.FLEECE_COLOR.get(), color);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("color.minecraft." + color(stack).getName()).withStyle(ChatFormatting.GRAY));
    }
}
