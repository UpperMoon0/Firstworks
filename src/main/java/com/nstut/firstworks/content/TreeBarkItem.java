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
        stack.set(ModDataComponents.WOOD_TYPE.get(), woodType);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        String type = woodType(stack);
        String displayName = com.nstut.firstworks.registry.WoodTypeRegistry.getDisplayName(type);
        Component typeName = displayName != null
                ? Component.literal(displayName)
                : Component.translatableWithFallback("wood_type.firstworks." + type, formatWoodTypeName(type));
        return Component.translatable("item.firstworks.tree_bark_named", typeName);
    }

    public static String formatWoodTypeName(String woodType) {
        if (woodType == null || woodType.isEmpty()) return "Oak";
        String path = woodType.contains(":") ? woodType.split(":", 2)[1] : woodType;
        String[] words = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.nstut.firstworks.client.TreeBarkItemRenderer.INSTANCE;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.firstworks.tree_bark.obtain").withStyle(ChatFormatting.GRAY));
    }
}
