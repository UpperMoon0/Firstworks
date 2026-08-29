package com.nstut.firstworks.content.quern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record QuernGrindingRecipe(Ingredient ingredient, int inputCount, ItemStack result,
        int saddleStrokes, int rotaryDuration) implements Recipe<SingleRecipeInput> {
    public QuernGrindingRecipe {
        if (inputCount < 1 || saddleStrokes < 1 || rotaryDuration < 1 || result.isEmpty())
            throw new IllegalArgumentException("Quern recipe counts, work values, and result must be positive");
    }
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        // Match independently of batch size so the workstation can accept a
        // recipe batch one item at a time. The block entity gates processing.
        return ingredient.test(input.item());
    }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.QUERN_GRINDING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.QUERN_GRINDING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<QuernGrindingRecipe> {
        private static final MapCodec<QuernGrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(QuernGrindingRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(QuernGrindingRecipe::inputCount),
                ItemStack.CODEC.fieldOf("result").forGetter(QuernGrindingRecipe::result),
                Codec.intRange(1, 256).optionalFieldOf("saddle_strokes", 8).forGetter(QuernGrindingRecipe::saddleStrokes),
                Codec.intRange(1, 72000).optionalFieldOf("rotary_duration", 80).forGetter(QuernGrindingRecipe::rotaryDuration)
        ).apply(i, QuernGrindingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, QuernGrindingRecipe> STREAM_CODEC = StreamCodec.of(
                (b, r) -> { Ingredient.CONTENTS_STREAM_CODEC.encode(b, r.ingredient); b.writeVarInt(r.inputCount);
                    ItemStack.STREAM_CODEC.encode(b, r.result); b.writeVarInt(r.saddleStrokes); b.writeVarInt(r.rotaryDuration); },
                b -> new QuernGrindingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(b), b.readVarInt(),
                        ItemStack.STREAM_CODEC.decode(b), b.readVarInt(), b.readVarInt()));
        @Override public MapCodec<QuernGrindingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, QuernGrindingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
