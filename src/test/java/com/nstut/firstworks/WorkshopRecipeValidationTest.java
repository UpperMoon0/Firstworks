package com.nstut.firstworks;

import com.nstut.firstworks.content.workshop.WorkshopRecipe;
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
        // This unit test only exercises station validation. Avoid touching Minecraft's global item
        // registries here; those are bootstrapped by the GameTest environment, not plain JUnit.
        return new WorkshopRecipe(station, null, 1, null, 1, false, null, 20);
    }
}
