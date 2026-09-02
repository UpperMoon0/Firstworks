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
    private static final Path JAVA = Path.of("src/main/java/com/nstut/firstworks");

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

    @Test
    public void shapedWorkstationsUseGeometryMatchedCollision() throws Exception {
        for (String path : List.of(
                "content/BellowsBlock.java",
                "content/workshop/PotteryWheelBlock.java",
                "content/workshop/StoneAnvilBlock.java")) {
            String src = Files.readString(JAVA.resolve(path));
            assertTrue(src.contains("getCollisionShape"), "Missing custom collision shape: " + path);
            assertTrue(src.contains("VoxelShape"), "Workstation collision must use explicit geometry: " + path);
        }
    }

    @Test
    public void resinScarShowsFacingAndGrowthStateInWorld() throws Exception {
        String blockstate = Files.readString(ASSETS.resolve("blockstates/resin_scar.json"));
        for (int age = 0; age <= 3; age++) {
            for (String facing : List.of("north", "east", "south", "west")) {
                assertTrue(blockstate.contains("age=" + age + ",facing=" + facing),
                        "Missing resin scar state age=" + age + " facing=" + facing);
            }
        }
        for (String model : List.of("resin_scar.json", "resin_scar_1.json", "resin_scar_2.json", "resin_scar_3.json")) {
            assertTrue(Files.exists(ASSETS.resolve("models/block/" + model)),
                    "Missing resin scar growth model: " + model);
        }
    }
}
