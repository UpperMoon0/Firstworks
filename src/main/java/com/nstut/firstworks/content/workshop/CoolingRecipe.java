package com.nstut.firstworks.content.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.Optional;

/** Physical cooling treatment. Infinite water never pays for a chemical reagent. */
public record CoolingRecipe(Ingredient ingredient, int count, String medium, double minimumEntry,
        double completion, Optional<ItemStack> result, Optional<String> marker, boolean blockReheating,
        boolean preserveForgeProgress, Optional<String> excludedMarker, int priority) implements Recipe<SingleRecipeInput> {
    public CoolingRecipe {
        if (!Double.isFinite(minimumEntry) || !Double.isFinite(completion) || minimumEntry <= completion
                || completion < 20 || minimumEntry > 5000 || count < 1 || count > 64 || medium.isBlank()
                || result.filter(s -> s.isEmpty() || s.getCount() > s.getMaxStackSize()).isPresent())
            throw new IllegalArgumentException("Invalid cooling treatment");
    }
    public static final MapCodec<CoolingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(CoolingRecipe::ingredient),
            Codec.intRange(1,64).optionalFieldOf("input_count",1).forGetter(CoolingRecipe::count),
            Codec.STRING.fieldOf("medium").forGetter(CoolingRecipe::medium),
            Codec.doubleRange(20,5000).fieldOf("minimum_entry_temperature").forGetter(CoolingRecipe::minimumEntry),
            Codec.doubleRange(20,5000).fieldOf("completion_temperature").forGetter(CoolingRecipe::completion),
            ItemStack.CODEC.optionalFieldOf("result").forGetter(CoolingRecipe::result),
            Codec.STRING.optionalFieldOf("marker").forGetter(CoolingRecipe::marker),
            Codec.BOOL.optionalFieldOf("block_reheating",false).forGetter(CoolingRecipe::blockReheating),
            Codec.BOOL.optionalFieldOf("preserve_forge_progress",false).forGetter(CoolingRecipe::preserveForgeProgress),
            Codec.STRING.optionalFieldOf("excluded_marker").forGetter(CoolingRecipe::excludedMarker),
            Codec.INT.optionalFieldOf("priority",0).forGetter(CoolingRecipe::priority)
    ).apply(i,CoolingRecipe::new));
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()) && input.item().getCount() >= count; }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.map(ItemStack::copy).orElseGet(() -> input.item().copy()); }
    @Override public boolean canCraftInDimensions(int w,int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.orElse(ItemStack.EMPTY); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.COOLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.COOLING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    public static final class Serializer implements RecipeSerializer<CoolingRecipe> {
        @Override public MapCodec<CoolingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,CoolingRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()); }
    }
}
