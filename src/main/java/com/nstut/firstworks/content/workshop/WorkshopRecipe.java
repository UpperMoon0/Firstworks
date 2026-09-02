package com.nstut.firstworks.content.workshop;

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

import java.util.Set;

public record WorkshopRecipe(String station, Ingredient ingredient, int inputCount, Ingredient catalyst,
                             int catalystCount, boolean consumeCatalyst, ItemStack result, int work)
        implements Recipe<SingleRecipeInput> {
    public static final String POTTERY_WHEEL = "pottery_wheel";
    public static final String KILN = "kiln";
    public static final String STONE_ANVIL = "stone_anvil";
    public static final String CRUCIBLE_FURNACE = "crucible_furnace";
    private static final Set<String> VALID_STATIONS = Set.of(
            POTTERY_WHEEL, KILN, STONE_ANVIL, CRUCIBLE_FURNACE);

    public WorkshopRecipe {
        if (!VALID_STATIONS.contains(station)) {
            throw new IllegalArgumentException("Unknown workshop station: " + station);
        }
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= inputCount;
    }

    public boolean hasCatalyst() {
        return catalyst.getItems().length > 0;
    }

    public boolean catalystMatches(ItemStack stack) {
        return !hasCatalyst() || catalyst.test(stack) && stack.getCount() >= catalystCount;
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
        return ModRecipes.WORKSHOP_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WORKSHOP_PROCESSING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<WorkshopRecipe> {
        private static final MapCodec<WorkshopRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("station").forGetter(WorkshopRecipe::station),
                Ingredient.CODEC.fieldOf("ingredient").forGetter(WorkshopRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(WorkshopRecipe::inputCount),
                Ingredient.CODEC.optionalFieldOf("catalyst", Ingredient.EMPTY).forGetter(WorkshopRecipe::catalyst),
                Codec.intRange(1, 64).optionalFieldOf("catalyst_count", 1).forGetter(WorkshopRecipe::catalystCount),
                Codec.BOOL.optionalFieldOf("consume_catalyst", false).forGetter(WorkshopRecipe::consumeCatalyst),
                ItemStack.CODEC.fieldOf("result").forGetter(WorkshopRecipe::result),
                Codec.intRange(1, 72000).optionalFieldOf("work", 20).forGetter(WorkshopRecipe::work)
        ).apply(instance, WorkshopRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, WorkshopRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    buffer.writeUtf(recipe.station);
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.catalyst);
                    buffer.writeVarInt(recipe.catalystCount);
                    buffer.writeBoolean(recipe.consumeCatalyst);
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                    buffer.writeVarInt(recipe.work);
                },
                buffer -> new WorkshopRecipe(buffer.readUtf(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt(),
                        buffer.readBoolean(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt()));

        @Override
        public MapCodec<WorkshopRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WorkshopRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
