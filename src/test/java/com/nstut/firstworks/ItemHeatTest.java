package com.nstut.firstworks;

import com.nstut.firstworks.content.workshop.ItemHeat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemHeatTest {
    @Test void coolingUsesElapsedWorldTimeWithoutInventoryTicks() {
        var heat = new ItemHeat(1200, 1200, 100);
        assertEquals(1200, heat.remaining(100));
        assertEquals(600, heat.remaining(700));
        assertEquals(0, heat.remaining(5000));
        assertEquals(1200, heat.remaining(50));
    }
    @Test void warmHeatIsNotWorkableAndInvalidOverCapacityIsClamped() {
        assertEquals("cold", ItemHeat.state(0));
        assertEquals("warm", ItemHeat.state(0.24F));
        assertEquals("workable", ItemHeat.state(0.25F));
        assertEquals(1200, new ItemHeat(5000, 1200, 0).remaining(0));
    }
    @Test void profileCodecKeepsLegacyDefaultsAndRejectsInvalidRates() {
        var json=com.google.gson.JsonParser.parseString("{\"ticks\":1200,\"capacity\":1200,\"updated_at\":100}");
        var heat=ItemHeat.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
        assertEquals(1,heat.multiplier());assertEquals(20,heat.endpoint());assertEquals("",heat.dimension());
        var profiled=new ItemHeat(1200,1200,100,1000,1100,10,20,"minecraft:overworld","firstworks:water");
        var encoded=ItemHeat.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,profiled).getOrThrow();
        assertEquals(profiled,ItemHeat.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow());
        encoded.getAsJsonObject().addProperty("cooling_multiplier",0);
        assertTrue(ItemHeat.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).error().isPresent());
        assertEquals(60,new ItemHeat(1200,1200,100,1000,1100,10,20,"minecraft:overworld").remaining(160));
    }
}
