package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoodProgressionRecipeTest {
    private static final Path TOOL_BINDING = Path.of(
            "src/main/java/com/nstut/firstworks/ToolBindingRecipes.java");
    private static final Path VANILLA_RECIPE_DIR = Path.of("src/main/resources/data/minecraft/recipe");

    @Test
    public void grainProgressionRewritesWinningRecipesAtRuntime() throws Exception {
        String source = Files.readString(TOOL_BINDING);
        assertTrue(source.contains("if (grain)"),
                "grain progression rewrites must only run while the toggle is enabled");
        assertTrue(source.contains("addGrainProgressionRoutes(replacements)"),
                "enabled grain progression must install its runtime recipe replacements");
        assertTrue(source.contains("Ingredient.of(WHEAT_DOUGHS)"),
                "Bread/Cookie progression must use the common wheat dough tag");
        assertTrue(source.contains("Ingredient.of(WHEAT_FLOURS)"),
                "Cake progression must use the common wheat flour tag");
        assertTrue(source.contains("shapedMisc(Items.COOKIE, 8"),
                "Cookie progression must retain the vanilla eight-cookie output");
        assertTrue(source.contains("\"AAA\", \"BCB\", \"###\""),
                "Cake progression must retain the vanilla three-row shape");
    }

    @Test
    public void grainProgressionDoesNotShipHardMinecraftOverrides() {
        for (String id : new String[]{"bread", "cookie", "cake"}) {
            assertFalse(Files.exists(VANILLA_RECIPE_DIR.resolve(id + ".json")),
                    "0.0.14 must not ship a hard minecraft:" + id
                            + " replacement; disabling progression must preserve the winning datapack recipe");
        }
    }

    @Test
    public void testCommonFlourAndDoughTags() throws Exception {
        Path floursTag = Path.of("src/main/resources/data/c/tags/item/flours.json");
        Path wheatFloursTag = Path.of("src/main/resources/data/c/tags/item/flours/wheat.json");
        Path doughsTag = Path.of("src/main/resources/data/c/tags/item/doughs.json");
        Path wheatDoughsTag = Path.of("src/main/resources/data/c/tags/item/doughs/wheat.json");

        assertTrue(Files.exists(floursTag) && Files.readString(floursTag).contains("firstworks:flour"));
        assertTrue(Files.exists(wheatFloursTag) && Files.readString(wheatFloursTag).contains("firstworks:flour"));
        assertTrue(Files.exists(doughsTag) && Files.readString(doughsTag).contains("firstworks:dough"));
        assertTrue(Files.exists(wheatDoughsTag) && Files.readString(wheatDoughsTag).contains("firstworks:dough"));
    }

    @Test
    public void testWaterBottleDoughRecipeSpecificallyMatchesWater() throws Exception {
        Path path = Path.of("src/main/resources/data/firstworks/recipe/dough_from_water_bottle.json");
        assertTrue(Files.exists(path), "dough_from_water_bottle.json must exist");
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String raw = json.toString();
            assertTrue(raw.contains("neoforge:components"), "Must use neoforge:components ingredient type");
            assertTrue(raw.contains("minecraft:potion"), "Must target minecraft:potion");
            assertTrue(raw.contains("minecraft:potion_contents"), "Must check potion_contents component");
            assertTrue(raw.contains("minecraft:water"), "Must specifically match water potion contents");
        }
    }

    @Test
    public void testDoughCookingRecipesProduceBread() throws Exception {
        String[] cookingRecipes = {
            "src/main/resources/data/firstworks/recipe/bread_from_campfire_dough.json",
            "src/main/resources/data/firstworks/recipe/bread_from_smelting_dough.json",
            "src/main/resources/data/firstworks/recipe/bread_from_smoking_dough.json"
        };

        for (String recipePath : cookingRecipes) {
            Path path = Path.of(recipePath);
            assertTrue(Files.exists(path), "Cooking recipe must exist: " + recipePath);
            try (FileReader reader = new FileReader(path.toFile())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue(json.toString().contains("c:doughs/wheat"),
                        "Cooking recipe must use c:doughs/wheat: " + recipePath);
                assertEquals("minecraft:bread", json.getAsJsonObject("result").get("id").getAsString());
            }
        }
    }
}
