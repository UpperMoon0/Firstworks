package com.nstut.firstworks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the 0.0.14 workshop art pass against accidental placeholder regressions. */
public class WorkshopVisualAssetTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/firstworks");

    @Test
    public void workstationModelsAreNotPlaceholderCubes() throws Exception {
        for (String name : List.of(
                "bellows", "pottery_wheel", "kiln", "stone_anvil",
                "crucible_furnace", "copper_loom", "rotary_quern")) {
            Path model = ASSETS.resolve("models/block/" + name + ".json");
            assertTrue(Files.exists(model), "Missing workstation model: " + name);
            String json = Files.readString(model);
            assertFalse(json.contains("minecraft:block/cube_all"), "Placeholder cube model: " + name);
            assertTrue(json.contains("\"elements\""), "Workstation must define real geometry: " + name);
        }
    }

    @Test
    public void animatedWorkstationsHaveDedicatedMovingParts() {
        for (String path : List.of(
                "models/block/bellows_bag.json",
                "models/block/bellows_top.json",
                "models/block/pottery_wheel_head.json",
                "models/block/kiln_embers.json",
                "models/block/crucible_furnace_contents.json",
                "models/block/copper_loom_beater.json",
                "models/block/rotary_quern_runner.json")) {
            assertTrue(Files.exists(ASSETS.resolve(path)), "Missing animated model part: " + path);
        }
    }

    @Test
    public void workshopInventoryModelsAreThreeDimensional() throws Exception {
        for (String name : List.of(
                "bellows", "pottery_wheel", "kiln", "stone_anvil", "crucible_furnace",
                "copper_loom", "rotary_quern", "unfired_casting_mold", "casting_mold",
                "unfired_crucible", "crucible", "unfired_tuyere", "tuyere",
                "unfired_refractory_brick", "refractory_brick")) {
            Path model = ASSETS.resolve("models/item/" + name + ".json");
            assertTrue(Files.exists(model), "Missing item model: " + name);
            String json = Files.readString(model);
            assertFalse(json.contains("minecraft:item/generated"), "Flat generated item model: " + name);
        }
    }
}
