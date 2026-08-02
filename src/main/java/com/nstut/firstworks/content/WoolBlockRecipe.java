package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class WoolBlockRecipe extends CustomRecipe {
    public WoolBlockRecipe() { super(CraftingBookCategory.BUILDING); }

    @Override public boolean matches(CraftingInput input, Level level) { return color(input) != null; }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        DyeColor color = color(input);
        return color == null ? ItemStack.EMPTY : new ItemStack(TextileColors.wool(color));
    }

    private static DyeColor color(CraftingInput input) {
        DyeColor color = null;
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!stack.is(ModItems.CLEAN_WOOL.get())) return null;
            DyeColor found = ColoredFleeceItem.color(stack);
            if (color != null && color != found) return null;
            color = found;
            count++;
        }
        return count == 4 ? color : null;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 4; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.WOOL_BLOCK_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<WoolBlockRecipe> {
        private static final MapCodec<WoolBlockRecipe> CODEC = MapCodec.unit(WoolBlockRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, WoolBlockRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new WoolBlockRecipe());
        @Override public MapCodec<WoolBlockRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, WoolBlockRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
