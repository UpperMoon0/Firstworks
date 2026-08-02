package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class SpinningRecipe extends ShapelessRecipe {
    public SpinningRecipe() {
        super("", CraftingBookCategory.MISC, new ItemStack(ModItems.TWINE.get(), 2),
                NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(ModItems.HAND_SPINDLE.get()),
                        Ingredient.of(ModItems.RETTED_FIBRE.get()),
                        Ingredient.of(ModItems.RETTED_FIBRE.get())));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(ModItems.HAND_SPINDLE.get())) {
                remaining.set(slot, stack.copyWithCount(1));
                break;
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPINNING_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<SpinningRecipe> {
        private static final MapCodec<SpinningRecipe> CODEC = MapCodec.unit(SpinningRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new SpinningRecipe());

        @Override public MapCodec<SpinningRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, SpinningRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
