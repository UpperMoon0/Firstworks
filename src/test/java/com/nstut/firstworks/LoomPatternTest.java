package com.nstut.firstworks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.nstut.firstworks.content.loom.LoomRecipe.Weaving;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class LoomPatternTest {
    @Test void defaultsAndExplicitPatternsRoundTrip() {
        assertEquals(Weaving.DEFAULT, Weaving.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        var pattern = new Weaving(List.of("A", "A", "B", "B"), Optional.of(8));
        var json = Weaving.CODEC.encodeStart(JsonOps.INSTANCE, pattern).getOrThrow();
        assertEquals(pattern, Weaving.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test void invalidPatternsAndPassCountsAreRejected() {
        for (String json : List.of("{\"pattern\":[]}", "{\"pattern\":[\"C\"]}",
                "{\"passes\":0}", "{\"passes\":65}", "{\"pattern\":[\"a\"]}")) {
            assertTrue(Weaving.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).error().isPresent(), json);
        }
        assertThrows(IllegalArgumentException.class, () -> new Weaving(java.util.Collections.nCopies(17, "A"), Optional.empty()));
    }
}
