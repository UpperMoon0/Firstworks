package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.FirstworksConfig;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class TextileBedRecipe extends CustomRecipe {
    public TextileBedRecipe() { super(CraftingBookCategory.MISC); }

    @Override public boolean matches(CraftingInput input, Level level) { return color(input) != null; }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        DyeColor color = color(input);
        return color == null ? ItemStack.EMPTY : new ItemStack(TextileColors.bed(color));
    }

    private static DyeColor color(CraftingInput input) {
        if (!FirstworksConfig.ENABLE_TEXTILE_PROGRESSION.getAsBoolean() || input.width() != 3 || input.height() != 3) return null;
        DyeColor color = null;
        for (int x = 0; x < 3; x++) {
            if (!input.getItem(x, 0).is(ModItems.CLOTH.get())) return null;
            ItemStack wool = input.getItem(x, 1);
            if (!wool.is(ModItems.CLEAN_WOOL.get())) return null;
            DyeColor found = ColoredFleeceItem.color(wool);
            if (color != null && color != found) return null;
            color = found;
            if (!input.getItem(x, 2).is(ItemTags.PLANKS)) return null;
        }
        return color;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width >= 3 && height >= 3; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.TEXTILE_BED_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<TextileBedRecipe> {
        private static final MapCodec<TextileBedRecipe> CODEC = MapCodec.unit(TextileBedRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, TextileBedRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new TextileBedRecipe());
        @Override public MapCodec<TextileBedRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, TextileBedRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
