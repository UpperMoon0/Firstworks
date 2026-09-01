package com.nstut.firstworks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoneCopperProgressionRecipeTest {
    private static String resource(String path) throws Exception {
        return Files.readString(Path.of("src/main/resources", path));
    }

    @Test
    public void workedCopperOwnsFastenersAndTools() throws Exception {
        String fasteners = resource("data/firstworks/recipe/copper_fasteners.json");
        assertTrue(fasteners.contains("firstworks:worked_copper_billet"));
        assertFalse(fasteners.contains("minecraft:copper_ingot"));

        String pickaxe = resource("data/firstworks/recipe/copper_pickaxe.json");
        assertTrue(pickaxe.contains("firstworks:worked_copper_billet"));
        assertTrue(pickaxe.contains("firstworks:strong_bindings"));
    }

    @Test
    public void primitiveCopperRequiresCastingAnnealingAndWorking() throws Exception {
        String casting = resource("data/firstworks/recipe/furnace_cast_copper.json");
        assertTrue(casting.contains("\"station\": \"crucible_furnace\""));
        assertTrue(casting.contains("firstworks:casting_mold"));
        assertTrue(casting.contains("minecraft:raw_copper"));

        String annealing = resource("data/firstworks/recipe/kiln_anneal_copper.json");
        assertTrue(annealing.contains("firstworks:cast_copper_billet"));
        assertTrue(annealing.contains("firstworks:annealed_copper_billet"));

        String working = resource("data/firstworks/recipe/anvil_work_copper.json");
        assertTrue(working.contains("firstworks:annealed_copper_billet"));
        assertTrue(working.contains("firstworks:worked_copper_billet"));
    }

    @Test
    public void stoneCompletionAndAdvancedCeramicsRemainReachable() throws Exception {
        assertTrue(resource("data/firstworks/tags/block/resin_trees.json").contains("minecraft:spruce_logs"));
        assertTrue(resource("data/firstworks/tags/item/strong_bindings.json").contains("firstworks:hafting_compound"));
        assertTrue(resource("data/firstworks/recipe/hafting_compound.json").contains("firstworks:resin"));
        assertTrue(resource("data/firstworks/recipe/quern_grog.json").contains("minecraft:brick"));
        assertTrue(resource("data/firstworks/recipe/kiln_crucible.json").contains("firstworks:unfired_crucible"));
        assertTrue(resource("data/firstworks/recipe/heavy_leather.json").contains("firstworks:tannin_solution"));
    }

    @Test
    public void bellowsHasNoCopperBootstrapCycle() throws Exception {
        String bellows = resource("data/firstworks/recipe/bellows.json");
        assertTrue(bellows.contains("firstworks:heavy_leather"));
        assertTrue(bellows.contains("firstworks:strong_bindings"));
        assertFalse(bellows.contains("firstworks:copper_fasteners"));
    }
}
