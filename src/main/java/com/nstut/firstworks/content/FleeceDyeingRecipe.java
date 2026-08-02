package com.nstut.firstworks.content;

import com.mojang.serialization.MapCodec;
import com.nstut.firstworks.registry.ModDataComponents;
import com.nstut.firstworks.registry.ModItems;
import com.nstut.firstworks.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class FleeceDyeingRecipe extends CustomRecipe {
    public FleeceDyeingRecipe() { super(CraftingBookCategory.MISC); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return assemble(input, level.registryAccess()).getCount() > 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Item fleeceItem = null;
        DyeItem dye = null;
        int fleeceCount = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.RAW_FLEECE.get()) || stack.is(ModItems.CLEAN_WOOL.get())) {
                if (fleeceItem != null && fleeceItem != stack.getItem()) return ItemStack.EMPTY;
                fleeceItem = stack.getItem();
                fleeceCount++;
            } else if (stack.getItem() instanceof DyeItem foundDye && dye == null) {
                dye = foundDye;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (fleeceItem == null || dye == null || fleeceCount < 1 || fleeceCount > 8) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(fleeceItem, fleeceCount);
        if (dye.getDyeColor() != net.minecraft.world.item.DyeColor.WHITE) {
            result.set(ModDataComponents.FLEECE_COLOR.get(), dye.getDyeColor());
        }
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.FLEECE_DYEING_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<FleeceDyeingRecipe> {
        private static final MapCodec<FleeceDyeingRecipe> CODEC = MapCodec.unit(FleeceDyeingRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, FleeceDyeingRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new FleeceDyeingRecipe());
        @Override public MapCodec<FleeceDyeingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FleeceDyeingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
