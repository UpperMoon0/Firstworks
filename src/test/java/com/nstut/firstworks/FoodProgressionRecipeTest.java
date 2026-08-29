package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FoodProgressionRecipeTest {

    @Test
    public void testBreadRecipeRequiresDoughTag() throws Exception {
        Path path = Path.of("src/main/resources/data/minecraft/recipe/bread.json");
        assertTrue(Files.exists(path), "bread.json override must exist");
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String raw = json.toString();
            assertFalse(raw.contains("minecraft:wheat"), "Bread recipe must not accept raw wheat");
            assertTrue(raw.contains("c:doughs/wheat"), "Bread recipe must require c:doughs/wheat");
        }
    }

    @Test
    public void testCookieRecipeRequiresDoughTag() throws Exception {
        Path path = Path.of("src/main/resources/data/minecraft/recipe/cookie.json");
        assertTrue(Files.exists(path), "cookie.json override must exist");
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String raw = json.toString();
            assertFalse(raw.contains("minecraft:wheat"), "Cookie recipe must not accept raw wheat");
            assertTrue(raw.contains("c:doughs/wheat"), "Cookie recipe must require c:doughs/wheat");
        }
    }

    @Test
    public void testCakeRecipeRequiresFlourTag() throws Exception {
        Path path = Path.of("src/main/resources/data/minecraft/recipe/cake.json");
        assertTrue(Files.exists(path), "cake.json override must exist");
        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String raw = json.toString();
            assertFalse(raw.contains("minecraft:wheat"), "Cake recipe must not accept raw wheat");
            assertTrue(raw.contains("c:flours/wheat"), "Cake recipe must require c:flours/wheat");
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
    public void testDoughCookingRecipesProduceBread() throws Exception {
        String[] cookingRecipes = {
            "src/main/resources/data/firstworks/recipe/bread_from_campfire_dough.json",
            "src/main/resources/data/firstworks/recipe/bread_from_smelting_dough.json",
            "src/main/resources/data/firstworks/recipe/bread_from_smoking_dough.json"
        };

        for (String r : cookingRecipes) {
            Path path = Path.of(r);
            assertTrue(Files.exists(path), "Cooking recipe must exist: " + r);
            try (FileReader reader = new FileReader(path.toFile())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue(json.toString().contains("c:doughs/wheat"), "Cooking recipe must use c:doughs/wheat: " + r);
                assertEquals("minecraft:bread", json.getAsJsonObject("result").get("id").getAsString());
            }
        }
    }
}