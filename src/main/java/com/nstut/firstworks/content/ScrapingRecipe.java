package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class ScrapingRecipe extends ShapelessRecipe {
    public ScrapingRecipe() {
        super("", CraftingBookCategory.MISC, new ItemStack(ModItems.SCRAPED_HIDE.get()),
                NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(ModItems.SOAKED_HIDE.get()),
                        Ingredient.of(ItemTags.SWORDS)));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.is(ItemTags.SWORDS)) continue;
            ItemStack sword = stack.copyWithCount(1);
            if (sword.isDamageableItem()) {
                int damage = sword.getDamageValue() + 1;
                if (damage >= sword.getMaxDamage()) continue;
                sword.setDamageValue(damage);
            }
            remaining.set(slot, sword);
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SCRAPING_SERIALIZER.get();
    }

    public static final class Serializer implements RecipeSerializer<ScrapingRecipe> {
        private static final MapCodec<ScrapingRecipe> CODEC = MapCodec.unit(ScrapingRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, ScrapingRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new ScrapingRecipe());

        @Override public MapCodec<ScrapingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ScrapingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
