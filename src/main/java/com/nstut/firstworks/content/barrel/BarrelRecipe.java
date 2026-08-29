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

import com.mojang.datafixers.util.Either;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public record BarrelRecipe(Ingredient ingredient, int inputCount, FluidIngredient fluid, int fluidAmount,
        ItemStack result, ResourceLocation outputFluid, int outputFluidAmount, int duration, boolean sealed)
        implements Recipe<SingleRecipeInput> {

    public static final ResourceLocation NO_FLUID = ResourceLocation.withDefaultNamespace("empty");

    public record FluidIngredient(Either<ResourceLocation, TagKey<Fluid>> target) {
        public static final Codec<FluidIngredient> CODEC = Codec.STRING.comapFlatMap(
                str -> {
                    try {
                        if (str.startsWith("#")) {
                            ResourceLocation loc = ResourceLocation.parse(str.substring(1));
                            return com.mojang.serialization.DataResult.success(new FluidIngredient(Either.right(TagKey.create(Registries.FLUID, loc))));
                        }
                        ResourceLocation loc = ResourceLocation.parse(str);
                        return com.mojang.serialization.DataResult.success(new FluidIngredient(Either.left(loc)));
                    } catch (Exception e) {
                        return com.mojang.serialization.DataResult.error(() -> "Invalid fluid or fluid tag location in barrel recipe: '" + str + "': " + e.getMessage());
                    }
                },
                ing -> ing.target.map(ResourceLocation::toString, tag -> "#" + tag.location())
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = StreamCodec.of(
                (buffer, ing) -> buffer.writeUtf(ing.target.map(ResourceLocation::toString, tag -> "#" + tag.location())),
                buffer -> {
                    String str = buffer.readUtf();
                    if (str.startsWith("#")) {
                        return new FluidIngredient(Either.right(TagKey.create(Registries.FLUID, ResourceLocation.parse(str.substring(1)))));
                    }
                    return new FluidIngredient(Either.left(ResourceLocation.parse(str)));
                }
        );

        public static FluidIngredient of(String str) {
            if (str.startsWith("#")) {
                return new FluidIngredient(Either.right(TagKey.create(Registries.FLUID, ResourceLocation.parse(str.substring(1)))));
            }
            return new FluidIngredient(Either.left(ResourceLocation.parse(str)));
        }

        public static FluidIngredient of(ResourceLocation loc) {
            return new FluidIngredient(Either.left(loc));
        }

        public boolean test(FluidStack stack) {
            if (stack.isEmpty()) return false;
            Fluid f = stack.getFluid();
            return target.map(
                    exactId -> BuiltInRegistries.FLUID.getKey(f).equals(exactId),
                    f::is
            );
        }

        public String asString() {
            return target.map(ResourceLocation::toString, tag -> "#" + tag.location());
        }
    }

    public boolean matchesFluid(FluidStack stack) {
        return fluid.test(stack);
    }

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
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(BarrelRecipe::inputCount),
                FluidIngredient.CODEC.fieldOf("fluid").forGetter(BarrelRecipe::fluid),
                Codec.intRange(1, 4000).fieldOf("fluid_amount").forGetter(BarrelRecipe::fluidAmount),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(BarrelRecipe::result),
                ResourceLocation.CODEC.optionalFieldOf("output_fluid", NO_FLUID).forGetter(BarrelRecipe::outputFluid),
                Codec.intRange(0, 4000).optionalFieldOf("output_fluid_amount", 0).forGetter(BarrelRecipe::outputFluidAmount),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 200).forGetter(BarrelRecipe::duration),
                Codec.BOOL.optionalFieldOf("sealed", true).forGetter(BarrelRecipe::sealed)
        ).apply(instance, BarrelRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BarrelRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    FluidIngredient.STREAM_CODEC.encode(buffer, recipe.fluid);
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
                        FluidIngredient.STREAM_CODEC.decode(buffer),
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
