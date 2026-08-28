package com.nstut.firstworks.content;

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

public record MortarGrindingRecipe(Ingredient ingredient, int inputCount, ItemStack result, int duration)
        implements Recipe<SingleRecipeInput> {
    public static final int MAX_INPUT_COUNT = 64;
    public static final int MAX_DURATION = 72_000;

    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.MORTAR_GRINDING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.MORTAR_GRINDING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<MortarGrindingRecipe> {
        private static final MapCodec<MortarGrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(MortarGrindingRecipe::ingredient),
                Codec.intRange(1, MAX_INPUT_COUNT).optionalFieldOf("input_count", 1)
                        .forGetter(MortarGrindingRecipe::inputCount),
                ItemStack.CODEC.fieldOf("result").forGetter(MortarGrindingRecipe::result),
                Codec.intRange(1, MAX_DURATION).optionalFieldOf("duration", 40)
                        .forGetter(MortarGrindingRecipe::duration)
        ).apply(instance, MortarGrindingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, MortarGrindingRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                    buffer.writeVarInt(recipe.duration);
                },
                buffer -> new MortarGrindingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt()));
        @Override public MapCodec<MortarGrindingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MortarGrindingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
