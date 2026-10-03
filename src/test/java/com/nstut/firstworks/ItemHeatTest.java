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
}
