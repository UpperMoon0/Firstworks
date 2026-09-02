package com.nstut.firstworks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackmakerCustomizationTest {
    private static final Path CONFIG = Path.of("src/main/java/com/nstut/firstworks/FirstworksConfig.java");
    private static final Path TOOL_BINDING = Path.of("src/main/java/com/nstut/firstworks/ToolBindingRecipes.java");
    private static final Path QUERN_RECIPE = Path.of("src/main/java/com/nstut/firstworks/content/quern/QuernGrindingRecipe.java");
    private static final Path QUERN_BE = Path.of("src/main/java/com/nstut/firstworks/content/quern/QuernBlockEntity.java");
    private static final Path WORKSHOP_BE = Path.of(
            "src/main/java/com/nstut/firstworks/content/workshop/WorkshopBlockEntity.java");
    private static final Path WORKSHOP_BLOCK = Path.of(
            "src/main/java/com/nstut/firstworks/content/workshop/WorkshopBlock.java");
    private static final Path JEI_PLUGIN = Path.of(
            "src/main/java/com/nstut/firstworks/compat/jei/FirstworksJeiPlugin.java");
    private static final Path WORKSHOP_JEI_PLUGIN = Path.of(
            "src/main/java/com/nstut/firstworks/compat/jei/WorkshopJeiPlugin.java");
    private static final Path VANILLA_RECIPE_DIR = Path.of("src/main/resources/data/minecraft/recipe");

    @Test
    public void grainProgressionHasConfigToggle() throws Exception {
        String config = Files.readString(CONFIG);
        assertTrue(config.contains("ENABLE_GRAIN_PROGRESSION"),
                "FirstworksConfig must expose enableGrainProgression");
        assertTrue(config.contains(".define(\"enableGrainProgression\", true)"),
                "grain progression must default to enabled");
    }

    @Test
    public void grainProgressionIsRuntimeOnlyAndPreservesDatapackWinnersWhenDisabled() throws Exception {
        String src = Files.readString(TOOL_BINDING);
        assertTrue(src.contains("OnDatapackSyncEvent"),
                "grain recipe gating must reuse the datapack-sync rewrite pattern");
        assertTrue(src.contains("ENABLE_GRAIN_PROGRESSION"),
                "the grain toggle must drive the rewrite");
        assertTrue(src.contains("if (grain)") && src.contains("addGrainProgressionRoutes"),
                "Firstworks grain routes must only be injected while progression is enabled");
        assertTrue(src.contains("WHEAT_DOUGHS") && src.contains("WHEAT_FLOURS"),
                "runtime grain routes must keep using the common wheat dough/flour tags");
        assertTrue(src.contains("shapedMisc(Items.COOKIE, 8"),
                "progression cookies must retain the vanilla eight-cookie output");
        assertTrue(src.contains("\"AAA\", \"BCB\", \"###\""),
                "progression cake must retain the expected three-row shape");

        for (String id : new String[]{"bread", "cookie", "cake"}) {
            assertFalse(Files.exists(VANILLA_RECIPE_DIR.resolve(id + ".json")),
                    "Firstworks must not ship a hard minecraft:" + id
                            + " datapack replacement; disabled progression must leave the winning datapack recipe untouched");
        }
    }

    @Test
    public void copperMachineUpgradesAreRegisteredAsJeiCatalysts() throws Exception {
        String src = Files.readString(JEI_PLUGIN);
        assertTrue(src.contains("ModItems.COPPER_HAND_SPINDLE.get(), SPINDLE_SPINNING"),
                "Copper Hand Spindle must expose spinning recipes in JEI");
        assertTrue(src.contains("ModBlocks.ROTARY_QUERN.get(), QUERN_GRINDING"),
                "Rotary Quern must expose quern recipes in JEI");
    }

    @Test
    public void workshopJeiRecipesAndCatalystsAreStationScoped() throws Exception {
        String src = Files.readString(WORKSHOP_JEI_PLUGIN);
        assertTrue(src.contains("POTTERY_WHEEL_PROCESSING")
                        && src.contains("KILN_PROCESSING")
                        && src.contains("STONE_ANVIL_PROCESSING")
                        && src.contains("CRUCIBLE_FURNACE_PROCESSING"),
                "workshop JEI integration must expose a recipe type per station");
        assertTrue(src.contains("filter(recipe -> station.equals(recipe.station()))"),
                "each JEI station category must receive only its matching workshop recipes");
        assertTrue(src.contains("ModBlocks.POTTERY_WHEEL.get(), POTTERY_WHEEL_PROCESSING"));
        assertTrue(src.contains("ModBlocks.KILN.get(), KILN_PROCESSING"));
        assertTrue(src.contains("ModBlocks.STONE_ANVIL.get(), STONE_ANVIL_PROCESSING"));
        assertTrue(src.contains("ModBlocks.CRUCIBLE_FURNACE.get(), CRUCIBLE_FURNACE_PROCESSING"));
        assertTrue(src.contains("ModBlocks.BELLOWS.get(), CRUCIBLE_FURNACE_PROCESSING"),
                "Bellows must expose only Crucible Furnace processing in JEI");
    }

    @Test
    public void workshopFuelOverlapHasExplicitGuiFreeRouting() throws Exception {
        String block = Files.readString(WORKSHOP_BLOCK);
        String entity = Files.readString(WORKSHOP_BE);
        assertTrue(entity.contains("preferredPlayerInsertionSlot"),
                "normal workshop insertion must resolve recipe roles deliberately");
        assertTrue(entity.contains("canInsertFuel") && entity.contains("insertFuel"),
                "heated workshops must expose an explicit fuel insertion path");
        assertTrue(block.contains("player.isShiftKeyDown() && workshop.canInsertFuel(stack)"),
                "sneak-right-click must force coal/charcoal into the fuel reserve when roles overlap");
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
