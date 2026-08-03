package com.nstut.firstworks.content.brick_mold;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record BrickMoldingRecipe(Ingredient ingredient, int inputCount, int presses, ItemStack result)
        implements Recipe<SingleRecipeInput> {
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.BRICK_MOLDING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.BRICK_MOLDING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<BrickMoldingRecipe> {
        private static final MapCodec<BrickMoldingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(BrickMoldingRecipe::ingredient),
                Codec.INT.optionalFieldOf("input_count", 1).forGetter(BrickMoldingRecipe::inputCount),
                Codec.INT.optionalFieldOf("presses", 1).forGetter(BrickMoldingRecipe::presses),
                ItemStack.CODEC.fieldOf("result").forGetter(BrickMoldingRecipe::result)
        ).apply(instance, BrickMoldingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, BrickMoldingRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    buffer.writeVarInt(recipe.presses);
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                },
                buffer -> new BrickMoldingRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        ItemStack.STREAM_CODEC.decode(buffer)));

        @Override public MapCodec<BrickMoldingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BrickMoldingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
