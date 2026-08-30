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
        long exactCalls = countOccurrences(method, "hurtAndBreak(1,");
        assertEquals(1, exactCalls,
                "fibre harvest must apply exactly one durability point (amount 1)");
        assertEquals(exactCalls, countOccurrences(method, "hurtAndBreak("),
                "fibre harvest must not apply any other durability amount");
        assertTrue(method.contains("if (guaranteed) {"),
                "fibre durability must only apply for guaranteed (knife) harvests");
    }

    @Test
    public void guaranteedOchreHarvestAppliesExactlyOneKnifeDurabilityPoint() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/nstut/firstworks/GameplayEvents.java"));
        String method = methodBody(source, "gatherRawOchre");

        // Regression guard from the PR #10 review: ochre harvesting must cost
        // exactly one durability point. The dedicated KnifeItem installs no Tool
        // component (see primitiveKnivesExtendTieredItemNotSwordItem), so ordinary
        // block breaking applies no knife durability; the only durability applied
        // here is this single explicit call. A second call, or a wrong amount,
        // would silently over-damage primitive knives.
        long exactCalls = countOccurrences(method, "hurtAndBreak(1,");
        assertEquals(1, exactCalls,
                "ochre harvest must apply exactly one durability point, not double-damage");
        assertEquals(exactCalls, countOccurrences(method, "hurtAndBreak("),
                "ochre harvest must not apply any other durability amount");

        // The single durability call must live inside the raw-ochre drop block.
        int dropIdx = method.indexOf("RAW_OCHRE");
        int dmgIdx = method.indexOf("hurtAndBreak");
        assertTrue(dropIdx >= 0 && dmgIdx > dropIdx,
                "ochre durability must follow the raw ochre drop, not be unconditional");
        assertTrue(method.contains("if (guaranteed) {"),
                "ochre durability must only apply for guaranteed (knife) harvests");
    }

    @Test
    public void primitiveKnivesExtendTieredItemNotSwordItem() throws Exception {
        String knife = Files.readString(Path.of("src/main/java/com/nstut/firstworks/content/KnifeItem.java"));
        String items = Files.readString(Path.of("src/main/java/com/nstut/firstworks/registry/ModItems.java"));
        // Architectural separation the review asked for: knives share the tier base
        // with SwordItem but are not swords.
        assertTrue(knife.contains("class KnifeItem extends TieredItem"));
        assertFalse(knife.contains("extends SwordItem"));
        // Handing the tier to TieredItem restores tier durability, repair material
        // (Bone/Flint), and enchantability — none of which plain Item provides.
        // TieredItem.isValidRepairItem delegates to tier.getRepairIngredient() and
        // getEnchantmentValue() to tier.getEnchantmentValue().
        assertTrue(knife.contains("super(tier,"), "tier must be delegated to TieredItem");
        // Combat costs exactly one durability per successful hit (SwordItem-equivalent).
        assertTrue(knife.contains("postHurtEnemy"));
        assertTrue(knife.contains("hurtAndBreak(1,"), "combat must cost exactly one durability");
        // Ordinary block breaking must cost 0: a knife installs no Tool component
        // (which is what supplies damagePerBlock in 1.21), and is not a SwordItem.
        // This is the real guarantee — a mineBlock override would be a red herring.
        assertFalse(knife.contains("createToolProperties"), "knife must not install a Tool component");
        assertFalse(knife.contains("Tool.of("), "knife must not install a Tool component");
        assertFalse(knife.contains("new Tool("), "knife must not install a Tool component");
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
