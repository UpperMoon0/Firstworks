package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

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
    public void catalystCodecTracksFieldPresenceInsteadOfResolvedItems() throws Exception {
        String source = Files.readString(WORKSHOP_RECIPE);
        assertTrue(source.contains("Optional<Ingredient> catalyst"),
                "workshop recipes must preserve whether the catalyst field was declared");
        assertTrue(source.contains("Ingredient.CODEC.optionalFieldOf(\"catalyst\")"),
                "catalyst presence must be represented by Optional rather than Ingredient.EMPTY as a default");
        assertTrue(source.contains("return catalyst.isPresent()"),
                "hasCatalyst must depend on field presence, not resolved tag contents");
        assertTrue(source.contains("buffer.writeBoolean(recipe.catalyst.isPresent())"),
                "network sync must preserve catalyst presence even when its resolved item set is empty");
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
