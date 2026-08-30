package com.nstut.firstworks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class TagInteropAndHarvestTest {
    private static final Path RECIPE_DIR = Path.of("src/main/resources/data/firstworks/recipe");

    @Test
    public void commonStringRecipesUseTheSharedTag() throws Exception {
        JsonObject basket = read("basket.json");
        assertEquals("c:strings", basket.getAsJsonObject("key").getAsJsonObject("T")
                .get("tag").getAsString());

        JsonObject cloth = read("weave_cloth_from_string.json");
        assertEquals("c:strings", cloth.getAsJsonObject("ingredient").get("tag").getAsString());
        assertFalse(Files.exists(RECIPE_DIR.resolve("weave_cloth_from_twine.json")),
                "Cloth must not have a duplicate twine recipe");

        JsonObject rope = read("rope.json");
        assertEquals(3, rope.getAsJsonArray("ingredients").size());
        rope.getAsJsonArray("ingredients").forEach(ingredient ->
                assertEquals("c:strings", ingredient.getAsJsonObject().get("tag").getAsString()));
        assertFalse(Files.exists(RECIPE_DIR.resolve("rope_from_string.json")),
                "Rope must not have a duplicate string recipe");
    }

    @Test
    public void plantFibreHarvestProtectsCreativePlayersAndDamagesKnives() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/nstut/firstworks/GameplayEvents.java"));
        String method = methodBody(source, "gatherPlantFibre");
        assertTrue(method.contains("event.getPlayer().isCreative()"));
        assertTrue(method.contains("tool.hurtAndBreak(1, event.getPlayer(), EquipmentSlot.MAINHAND)"));
    }

    @Test
    public void guaranteedOchreHarvestDamagesKnives() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/nstut/firstworks/GameplayEvents.java"));
        String method = methodBody(source, "gatherRawOchre");
        assertTrue(method.contains("tool.hurtAndBreak(1, event.getPlayer(), EquipmentSlot.MAINHAND)"));
    }

    private static JsonObject read(String fileName) throws Exception {
        try (FileReader reader = new FileReader(RECIPE_DIR.resolve(fileName).toFile())) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String methodBody(String source, String methodName) {
        int start = source.indexOf("void " + methodName + "(");
        assertTrue(start >= 0, "Missing method: " + methodName);
        int nextMethod = source.indexOf("\n    @SubscribeEvent", start + 1);
        return source.substring(start, nextMethod >= 0 ? nextMethod : source.length());
    }
}
