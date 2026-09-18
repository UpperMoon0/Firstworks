package com.nstut.firstworks.content.loom;

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
import java.util.List;
import java.util.Optional;
import com.mojang.serialization.DataResult;

public record LoomRecipe(Ingredient ingredient, int inputCount, ItemStack result, int strokes, Weaving weaving)
        implements Recipe<SingleRecipeInput> {
    public LoomRecipe(Ingredient ingredient, int inputCount, ItemStack result, int strokes) {
        this(ingredient, inputCount, result, strokes, Weaving.DEFAULT);
    }

    public int passes() { return weaving.passes().orElse(strokes); }
    public String requiredShed(int progress) {
        return weaving.pattern().get(Math.floorMod(progress, weaving.pattern().size()));
    }

    public record Weaving(List<String> pattern, Optional<Integer> passes) {
        public static final Weaving DEFAULT = new Weaving(List.of("A", "B"), Optional.empty());
        private static final Codec<String> SHED = Codec.STRING.validate(value ->
                value.equals("A") || value.equals("B") ? DataResult.success(value)
                        : DataResult.error(() -> "Loom shed must be A or B"));
        public static final Codec<Weaving> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SHED.listOf().validate(values -> !values.isEmpty() && values.size() <= 16
                        ? DataResult.success(values) : DataResult.error(() -> "Weave pattern needs 1 to 16 sheds"))
                        .optionalFieldOf("pattern", DEFAULT.pattern()).forGetter(Weaving::pattern),
                Codec.intRange(1, 64).optionalFieldOf("passes").forGetter(Weaving::passes)
        ).apply(instance, Weaving::new));

        public Weaving {
            pattern = List.copyOf(pattern);
            if (pattern.isEmpty() || pattern.size() > 16 || pattern.stream().anyMatch(s -> !s.equals("A") && !s.equals("B")))
                throw new IllegalArgumentException("Weave pattern needs 1 to 16 A/B sheds");
            if (passes.isPresent() && (passes.get() < 1 || passes.get() > 64))
                throw new IllegalArgumentException("Weave passes must be 1 to 64");
        }
    }
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
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.LOOM_WEAVING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.LOOM_WEAVING_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<LoomRecipe> {
        private static final MapCodec<LoomRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(LoomRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(LoomRecipe::inputCount),
                ItemStack.CODEC.fieldOf("result").forGetter(LoomRecipe::result),
                Codec.intRange(1, 64).optionalFieldOf("strokes", 16).forGetter(LoomRecipe::strokes),
                Weaving.CODEC.optionalFieldOf("weaving", Weaving.DEFAULT).forGetter(LoomRecipe::weaving)
        ).apply(instance, LoomRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, LoomRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                    buffer.writeVarInt(recipe.inputCount);
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                    buffer.writeVarInt(recipe.strokes);
                    buffer.writeUtf(String.join("", recipe.weaving.pattern()));
                    buffer.writeVarInt(recipe.weaving.passes().orElse(0));
                },
                buffer -> new LoomRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(),
                        ItemStack.STREAM_CODEC.decode(buffer),
                        buffer.readVarInt(), readWeaving(buffer)));

        private static Weaving readWeaving(RegistryFriendlyByteBuf buffer) {
            List<String> pattern = buffer.readUtf(16).chars().mapToObj(c -> String.valueOf((char) c)).toList();
            int passes = buffer.readVarInt();
            return new Weaving(pattern, passes == 0 ? Optional.empty() : Optional.of(passes));
        }

        @Override public MapCodec<LoomRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, LoomRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
