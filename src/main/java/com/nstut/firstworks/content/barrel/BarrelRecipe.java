package com.nstut.firstworks.content.barrel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record BarrelRecipe(Ingredient ingredient, int inputCount, ResourceLocation fluid, int fluidAmount,
        ItemStack result, ResourceLocation outputFluid, int outputFluidAmount, int duration, boolean sealed)
        implements Recipe<SingleRecipeInput> {

    public static final ResourceLocation NO_FLUID = ResourceLocation.withDefaultNamespace("empty");

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BARREL_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.BARREL_PROCESSING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<BarrelRecipe> {
        private static final MapCodec<BarrelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(BarrelRecipe::ingredient),
                Codec.INT.optionalFieldOf("input_count", 1).forGetter(BarrelRecipe::inputCount),
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(BarrelRecipe::fluid),
                Codec.INT.fieldOf("fluid_amount").forGetter(BarrelRecipe::fluidAmount),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(BarrelRecipe::result),
                ResourceLocation.CODEC.optionalFieldOf("output_fluid", NO_FLUID).forGetter(BarrelRecipe::outputFluid),
                Codec.INT.optionalFieldOf("output_fluid_amount", 0).forGetter(BarrelRecipe::outputFluidAmount),
                Codec.INT.optionalFieldOf("duration", 200).forGetter(BarrelRecipe::duration),
                Codec.BOOL.optionalFieldOf("sealed", true).forGetter(BarrelRecipe::sealed)
        ).apply(instance, BarrelRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BarrelRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe.fluid);
                    buffer.writeVarInt(recipe.fluidAmount);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.result);
                    ResourceLocation.STREAM_CODEC.encode(buffer, recipe.outputFluid);
                    buffer.writeVarInt(recipe.outputFluidAmount);
                    buffer.writeVarInt(recipe.duration);
                    buffer.writeBoolean(recipe.sealed);
                },
                buffer -> new BarrelRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        ResourceLocation.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                        ResourceLocation.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readBoolean()));

        @Override public MapCodec<BarrelRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BarrelRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
