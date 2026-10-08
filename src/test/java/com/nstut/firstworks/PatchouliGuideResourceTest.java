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
    void guideRecipeIsAlwaysAvailableAndTargetsTheBook() throws Exception {
        JsonObject recipe = JsonParser.parseString(
                Files.readString(ROOT.resolve("data/firstworks/recipe/field_guide.json"))).getAsJsonObject();
        assertFalse(recipe.has("neoforge:conditions"));

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
    @Test
    void allGuideEntriesHaveValidCategoriesAndRecipeReferences() throws Exception {
        Path content = ROOT.resolve(ASSET_ROOT);
        try (var files = Files.walk(content.resolve("entries"))) {
            var entries = files.filter(p -> p.toString().endsWith(".json")).toList();
            assertEquals(36, entries.size());
            for (Path file : entries) {
                var entry = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String category = entry.get("category").getAsString().substring("firstworks:".length());
                assertTrue(Files.exists(content.resolve("categories/" + category + ".json")), file.toString());
                assertFalse(entry.getAsJsonArray("pages").isEmpty(), file.toString());
                for (var element : entry.getAsJsonArray("pages")) {
                    var page = element.getAsJsonObject();
                    String type = page.get("type").getAsString();
                    assertTrue(List.of("patchouli:text", "patchouli:crafting", "patchouli:smelting").contains(type));
                    if (page.has("recipe")) {
                        String id = page.get("recipe").getAsString().substring("firstworks:".length());
                        assertTrue(Files.exists(ROOT.resolve("data/firstworks/recipe/" + id + ".json")), id);
                    } else assertFalse(page.get("text").getAsString().isBlank());
                }
            }
        }
        try (var files = Files.list(content.resolve("categories"))) { assertEquals(8, files.count()); }
    }

    @Test
    void bookTextureAndRequiredDependencyArePackagedCorrectly() throws Exception {
        JsonObject book = JsonParser.parseString(Files.readString(ROOT.resolve(BOOK))).getAsJsonObject();
        assertEquals("firstworks:field_guide", book.get("model").getAsString());
        JsonObject model = JsonParser.parseString(Files.readString(
                ROOT.resolve("assets/firstworks/models/item/field_guide.json"))).getAsJsonObject();
        assertEquals("firstworks:item/field_guide", model.getAsJsonObject("textures").get("layer0").getAsString());
        var image = javax.imageio.ImageIO.read(ROOT.resolve("assets/firstworks/textures/item/field_guide.png").toFile());
        assertEquals(64, image.getWidth()); assertEquals(64, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertTrue(Files.readString(Path.of("src/main/templates/META-INF/neoforge.mods.toml")).replace("\r", "")
                .contains("modId=\"patchouli\"\ntype=\"required\""));
        assertTrue(Files.readString(Path.of(".github/workflows/release.yml"))
                .contains("\"slug\":\"patchouli\",\"projectID\":306770,\"type\":\"requiredDependency\""));
    }

    @Test
    void everyBundledRecipeIsCoveredByTheReference() throws Exception {
        var crafting = JsonParser.parseString(Files.readString(ROOT.resolve(ASSET_ROOT +
                "entries/integration/crafting_reference.json"))).getAsJsonObject().getAsJsonArray("pages");
        var processing = JsonParser.parseString(Files.readString(ROOT.resolve(ASSET_ROOT +
                "entries/integration/processing_reference.json"))).getAsJsonObject().getAsJsonArray("pages");
        var recipeIds = new java.util.HashSet<String>();
        crafting.forEach(p -> recipeIds.add(p.getAsJsonObject().get("recipe").getAsString()));
        var titles = new java.util.HashSet<String>();
        processing.forEach(p -> titles.add(p.getAsJsonObject().get("title").getAsString().toLowerCase(java.util.Locale.ROOT)));
        try (var files = Files.list(ROOT.resolve("data/firstworks/recipe"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String type = recipe.get("type").getAsString();
                String id = file.getFileName().toString().replace(".json", "");
                if (type.startsWith("minecraft:crafting_") || type.equals("minecraft:smelting"))
                    assertTrue(recipeIds.contains("firstworks:" + id), id);
                else {
                    String title = id.replace('_', ' ');
                    if (title.length() > 40) title = title.substring(0, 40);
                    assertTrue(titles.contains(title), id);
                }
            }
        }
    }

}
