package com.nstut.firstworks.content.workshop;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.Objects;

/** Complete input state for a workshop recipe lookup, including station and catalyst. */
public record WorkshopRecipeInput(String station, ItemStack input, ItemStack catalyst) implements RecipeInput {
    public WorkshopRecipeInput {
        Objects.requireNonNull(station, "station");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(catalyst, "catalyst");
    }

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> input;
            case 1 -> catalyst;
            default -> throw new IndexOutOfBoundsException("Workshop recipe input index: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
