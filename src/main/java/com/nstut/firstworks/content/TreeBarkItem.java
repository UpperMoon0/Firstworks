package com.nstut.firstworks.content;

import com.nstut.firstworks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class TreeBarkItem extends Item {
    public TreeBarkItem(Properties properties) {
        super(properties);
    }

    public static String woodType(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WOOD_TYPE.get(), "oak");
    }

    public static ItemStack create(Item item, String woodType, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (!"oak".equals(woodType)) {
            stack.set(ModDataComponents.WOOD_TYPE.get(), woodType);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String type = woodType(stack);
        if ("oak".equals(type)) return super.getName(stack);
        String translationKey = "wood_type.firstworks." + type;
        Component typeName = Component.translatable(translationKey);
        return Component.translatable("item.firstworks.tree_bark_named", typeName);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.firstworks.tree_bark.obtain").withStyle(ChatFormatting.GRAY));
    }
}
