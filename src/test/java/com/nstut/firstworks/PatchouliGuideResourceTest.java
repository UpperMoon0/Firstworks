package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatchouliGuideResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final String BOOK = "data/firstworks/patchouli_books/field_guide/book.json";
    private static final String ASSET_ROOT = "assets/firstworks/patchouli_books/field_guide/en_us/";

    @Test
    void fieldGuideResourcesArePresentAndValidJson() throws Exception {
        List<String> paths = List.of(
                BOOK,
                ASSET_ROOT + "categories/getting_started.json",
                ASSET_ROOT + "categories/workstations.json",
                ASSET_ROOT + "entries/getting_started/first_steps.json",
                ASSET_ROOT + "entries/workstations/stone_anvil.json",
                ASSET_ROOT + "entries/workstations/loom.json",
                ASSET_ROOT + "entries/workstations/mortar.json",
                ASSET_ROOT + "entries/workstations/crucible.json",
                ASSET_ROOT + "entries/workstations/barrel.json",
                ASSET_ROOT + "entries/workstations/hand_spindle.json",
                "data/firstworks/recipe/field_guide.json"
        );

        for (String path : paths) {
            Path file = ROOT.resolve(path);
            assertTrue(Files.isRegularFile(file), "Missing guide resource: " + path);
            assertDoesNotThrow(() -> JsonParser.parseString(Files.readString(file)), "Invalid JSON: " + path);
        }
    }

    @Test
    void guideRecipeIsPatchouliConditionalAndTargetsTheBook() throws Exception {
        JsonObject recipe = JsonParser.parseString(
                Files.readString(ROOT.resolve("data/firstworks/recipe/field_guide.json"))).getAsJsonObject();

        var conditions = recipe.getAsJsonArray("neoforge:conditions");
        assertNotNull(conditions);
        assertTrue(conditions.asList().stream().anyMatch(element -> {
            JsonObject condition = element.getAsJsonObject();
            return "neoforge:mod_loaded".equals(condition.get("type").getAsString())
                    && "patchouli".equals(condition.get("modid").getAsString());
        }));

        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("patchouli:guide_book", result.get("id").getAsString());
        assertEquals("firstworks:field_guide",
                result.getAsJsonObject("components").get("patchouli:book").getAsString());
    }

    @Test
    void bookUsesResourcePackContentAndHasBothCoreCategories() throws Exception {
        JsonObject book = JsonParser.parseString(Files.readString(ROOT.resolve(BOOK))).getAsJsonObject();
        assertTrue(book.get("use_resource_pack").getAsBoolean());

        JsonObject firstSteps = JsonParser.parseString(Files.readString(
                ROOT.resolve(ASSET_ROOT + "entries/getting_started/first_steps.json"))).getAsJsonObject();
        assertEquals("firstworks:getting_started", firstSteps.get("category").getAsString());

        JsonObject anvil = JsonParser.parseString(Files.readString(
                ROOT.resolve(ASSET_ROOT + "entries/workstations/stone_anvil.json"))).getAsJsonObject();
        assertEquals("firstworks:workstations", anvil.get("category").getAsString());
    }
}
