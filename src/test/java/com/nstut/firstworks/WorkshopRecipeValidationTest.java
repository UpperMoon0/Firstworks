package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nstut.firstworks.content.workshop.WorkshopRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkshopRecipeValidationTest {
    private static final Path WORKSHOP_RECIPE = Path.of(
            "src/main/java/com/nstut/firstworks/content/workshop/WorkshopRecipe.java");
    private static final Path RECIPE_DIR = Path.of("src/main/resources/data/firstworks/recipe");
    private static final Set<String> VALID_STATIONS = Set.of(
            "pottery_wheel", "kiln", "stone_anvil", "crucible_furnace");

    @Test
    public void recipeContractRejectsUnknownStations() throws Exception {
        String source = Files.readString(WORKSHOP_RECIPE);
        assertTrue(source.contains("private static final Set<String> VALID_STATIONS = Set.of("),
                "WorkshopRecipe must keep an explicit station allow-list");
        assertTrue(source.contains("VALID_STATIONS.contains(station)"),
                "WorkshopRecipe constructor must validate station ids");
        assertTrue(source.contains("throw new IllegalArgumentException(\"Unknown workshop station:"),
                "unknown station ids must fail recipe construction/loading");
    }

    @Test
    public void declaredEmptyCatalystCannotCollapseIntoNoCatalyst() {
        WorkshopRecipe declaredEmpty = new WorkshopRecipe(
                WorkshopRecipe.POTTERY_WHEEL,
                Ingredient.EMPTY,
                1,
                Optional.of(Ingredient.EMPTY),
                1,
                false,
                ItemStack.EMPTY,
                1);
        WorkshopRecipe absent = new WorkshopRecipe(
                WorkshopRecipe.POTTERY_WHEEL,
                Ingredient.EMPTY,
                1,
                Optional.empty(),
                1,
                false,
                ItemStack.EMPTY,
                1);

        assertTrue(declaredEmpty.hasCatalyst(),
                "an explicitly declared empty catalyst must remain a declared requirement");
        assertFalse(declaredEmpty.catalystMatches(ItemStack.EMPTY),
                "an empty/unresolved declared catalyst must match nothing, not bypass the requirement");
        assertFalse(absent.hasCatalyst(), "an omitted catalyst must remain optional/absent");
        assertTrue(absent.catalystMatches(ItemStack.EMPTY),
                "recipes with no catalyst field must not require a catalyst");
    }

    @Test
    public void catalystCodecTracksFieldPresenceInsteadOfResolvedItems() throws Exception {
        String source = Files.readString(WORKSHOP_RECIPE);
        assertTrue(source.contains("Ingredient.CODEC.optionalFieldOf(\"catalyst\")"),
                "catalyst presence must be represented by Optional rather than Ingredient.EMPTY as a default");
        assertTrue(source.contains("return catalyst.isPresent()"),
                "hasCatalyst must depend on field presence, not resolved tag contents");
    }

    @Test
    public void shippedWorkshopRecipesUseOnlySupportedStations() throws Exception {
        try (Stream<Path> files = Files.list(RECIPE_DIR)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(WorkshopRecipeValidationTest::validateRecipe);
        }
    }

    private static void validateRecipe(Path path) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (!json.has("type") || !"firstworks:workshop_processing".equals(json.get("type").getAsString())) {
                return;
            }
            assertTrue(json.has("station"), "Workshop recipe missing station: " + path.getFileName());
            String station = json.get("station").getAsString();
            assertTrue(VALID_STATIONS.contains(station),
                    "Unsupported workshop station '" + station + "' in " + path.getFileName());
        } catch (Exception exception) {
            throw new AssertionError("Failed to validate " + path.getFileName(), exception);
        }
    }
}
