package com.nstut.firstworks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackmakerCustomizationTest {
    private static final Path CONFIG = Path.of("src/main/java/com/nstut/firstworks/FirstworksConfig.java");
    private static final Path TOOL_BINDING = Path.of("src/main/java/com/nstut/firstworks/ToolBindingRecipes.java");
    private static final Path QUERN_RECIPE = Path.of("src/main/java/com/nstut/firstworks/content/quern/QuernGrindingRecipe.java");
    private static final Path QUERN_BE = Path.of("src/main/java/com/nstut/firstworks/content/quern/QuernBlockEntity.java");

    @Test
    public void grainProgressionHasConfigToggle() throws Exception {
        String config = Files.readString(CONFIG);
        assertTrue(config.contains("ENABLE_GRAIN_PROGRESSION"),
                "FirstworksConfig must expose enableGrainProgression");
        assertTrue(config.contains(".define(\"enableGrainProgression\", true)"),
                "grain progression must default to enabled");
    }

    @Test
    public void grainOverridesRestoreExactVanillaRoutesWhenDisabled() throws Exception {
        String src = Files.readString(TOOL_BINDING);
        assertTrue(src.contains("OnDatapackSyncEvent"),
                "grain recipe gating must reuse the datapack-sync rewrite pattern");
        assertTrue(src.contains("ENABLE_GRAIN_PROGRESSION"),
                "the grain toggle must drive the rewrite");
        assertTrue(src.contains("addGrainVanillaRoutes"),
                "disabling grain progression must swap the overrides back to vanilla routes");
        assertTrue(src.contains("shapedVanilla(Items.COOKIE, 8"),
                "restored vanilla cookies must produce eight cookies");
        assertTrue(src.contains("\"AAA\", \"BCB\", \"###\""),
                "restored vanilla cake must consume three wheat across the bottom row");
    }

    @Test
    public void quernRecipeSupportsPriorityField() throws Exception {
        String src = Files.readString(QUERN_RECIPE);
        assertTrue(src.contains("int priority"), "QuernGrindingRecipe must carry a priority field");
        assertTrue(src.contains("optionalFieldOf(\"priority\", 0)"),
                "priority must be an optional codec field defaulting to 0");
        assertTrue(src.contains("b.writeVarInt(r.priority)"),
                "priority must be encoded on the network stream");
        assertTrue(src.contains("b.readVarInt(), b.readVarInt())"),
                "priority must be decoded on the network stream (last varint)");
    }

    @Test
    public void quernSelectionIsPriorityAware() throws Exception {
        String src = Files.readString(QUERN_BE);
        assertTrue(src.contains("bestRecipeFor"),
                "quern must select the best recipe via a shared helper");
        assertTrue(src.contains("Comparator.comparingInt") && src.contains("priority()"),
                "overlapping quern matchers must resolve by priority");
        assertTrue(src.contains("thenComparing"),
                "priority ties must break deterministically by recipe id");
    }

    @Test
    public void quernVisualSpeedScalesWithoutFixedCatchUpBacklog() throws Exception {
        String src = Files.readString(QUERN_BE);
        assertTrue(src.contains("lastVisualWork"),
                "quern animation must synchronize the latest applied work");
        assertTrue(src.contains("rotationSteps += workAmount"),
                "angular travel must remain proportional to work applied per crank");
        assertTrue(src.contains("diff * 0.65D"),
                "large rotation targets must accelerate catch-up instead of draining at a fixed rate");
        assertTrue(src.contains("Mth.clamp(quern.lastVisualWork * 9.0D"),
                "minimum visual speed must scale with applied work");
    }
}
