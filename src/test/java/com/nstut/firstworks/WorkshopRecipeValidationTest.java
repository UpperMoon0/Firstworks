package com.nstut.firstworks;

import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkshopRecipeValidationTest {
    @Test
    public void supportedStationsConstructNormally() {
        for (String station : new String[]{
                WorkshopRecipe.POTTERY_WHEEL,
                WorkshopRecipe.KILN,
                WorkshopRecipe.STONE_ANVIL,
                WorkshopRecipe.CRUCIBLE_FURNACE}) {
            assertDoesNotThrow(() -> recipe(station));
        }
    }

    @Test
    public void unknownStationFailsAtRecipeDecodeBoundary() {
        assertThrows(IllegalArgumentException.class, () -> recipe("typo_furnace"));
    }

    private static WorkshopRecipe recipe(String station) {
        return new WorkshopRecipe(station, Ingredient.of(Items.CLAY_BALL), 1, Ingredient.EMPTY,
                1, false, new ItemStack(Items.BRICK), 20);
    }
}
