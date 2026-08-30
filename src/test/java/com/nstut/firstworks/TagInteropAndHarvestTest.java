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
        long durabilityCalls = countOccurrences(method, "hurtAndBreak");
        assertEquals(1, durabilityCalls,
                "fibre harvest must apply exactly one durability point");
        assertTrue(method.contains("if (guaranteed) {"),
                "fibre durability must only apply for guaranteed (knife) harvests");
    }

    @Test
    public void guaranteedOchreHarvestAppliesExactlyOneKnifeDurabilityPoint() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/nstut/firstworks/GameplayEvents.java"));
        String method = methodBody(source, "gatherRawOchre");

        // Regression guard from the PR #10 review: ochre harvesting must cost
        // exactly one durability point. With dedicated KnifeItem (extending Item,
        // not SwordItem) there is no sword-style block mining damage, so the only
        // durability is this single explicit call. Two calls here, or leaving the
        // knife a SwordItem, would silently over-damage primitive knives.
        long durabilityCalls = countOccurrences(method, "hurtAndBreak");
        assertEquals(1, durabilityCalls,
                "ochre harvest must apply exactly one durability point, not double-damage");

        // The single durability call must live inside the raw-ochre drop block.
        int dropIdx = method.indexOf("RAW_OCHRE");
        int dmgIdx = method.indexOf("hurtAndBreak");
        assertTrue(dropIdx >= 0 && dmgIdx > dropIdx,
                "ochre durability must follow the raw ochre drop, not be unconditional");
        assertTrue(method.contains("if (guaranteed) {"),
                "ochre durability must only apply for guaranteed (knife) harvests");
    }

    @Test
    public void primitiveKnivesDoNotInheritSwordBlockDamage() throws Exception {
        String knife = Files.readString(Path.of("src/main/java/com/nstut/firstworks/content/KnifeItem.java"));
        String items = Files.readString(Path.of("src/main/java/com/nstut/firstworks/registry/ModItems.java"));
        assertTrue(knife.contains("class KnifeItem extends Item"));
        assertFalse(knife.contains("extends SwordItem"));
        // A mineBlock override is the mechanism by which SwordItem applies
        // damagePerBlock on every non-zero-hardness block break; knives must not.
        assertFalse(knife.contains("mineBlock"),
                "KnifeItem must not override mineBlock (no sword-style block durability)");
        assertTrue(items.contains("new KnifeItem(ModToolTiers.BONE"));
        assertTrue(items.contains("new KnifeItem(ModToolTiers.FLINT"));
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

    private static long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
