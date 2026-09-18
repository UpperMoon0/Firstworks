package com.nstut.firstworks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nstut.firstworks.content.mortar.MortarStage;
import com.nstut.firstworks.content.workshop.ForgeData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkstationMetadataTest {
    @Test void forgeDefaultsAndProfilesRoundTrip() {
        var json = JsonParser.parseString("{\"actions\":[\"flatten\",\"draw\",\"bend\"],\"visual\":{\"initial_profile\":\"billet\"}}");
        var data = ForgeData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(1200, data.heatTicks());
        assertEquals(data, ForgeData.CODEC.parse(JsonOps.INSTANCE, ForgeData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow());
    }
    @Test void invalidForgeRequirementsAreRejected() {
        for (String json : new String[]{"{\"actions\":[]}", "{\"actions\":[\"cut\"]}",
                "{\"actions\":[\"draw\"],\"heat_ticks\":-1}",
                "{\"actions\":[\"draw\"],\"visual\":{\"height\":0}}"})
            assertTrue(ForgeData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(), json);
    }
    @Test void stagesRequireMeaningfulActionSpecificWork() {
        for (String json : new String[]{"{\"action\":\"crush\",\"count\":3}", "{\"action\":\"grind\",\"duration\":40}"}) {
            var stage = MortarStage.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
            assertEquals(stage, MortarStage.CODEC.parse(JsonOps.INSTANCE, MortarStage.CODEC.encodeStart(JsonOps.INSTANCE, stage).getOrThrow()).getOrThrow());
        }
        for (String json : new String[]{"{\"action\":\"crush\"}", "{\"action\":\"grind\",\"count\":3}",
                "{\"action\":\"grind\",\"duration\":-1}", "{\"action\":\"crush\",\"count\":2,\"duration\":4}",
                "{\"action\":\"spin\",\"duration\":1}"})
            assertTrue(MortarStage.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(), json);
    }
}
