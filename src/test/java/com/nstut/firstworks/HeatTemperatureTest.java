package com.nstut.firstworks;

import com.nstut.firstworks.content.workshop.HeatTemperature;
import com.nstut.firstworks.content.workshop.TemperatureUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeatTemperatureTest {
    @Test void unitsConvertTheSameTemperature() {
        assertEquals("100°C", TemperatureUnit.CELSIUS.format(100));
        assertEquals("212°F", TemperatureUnit.FAHRENHEIT.format(100));
        assertEquals(373.15, TemperatureUnit.KELVIN.convert(100), 0.0001);
        assertEquals("373K", TemperatureUnit.KELVIN.format(100));
        assertEquals("68°F", TemperatureUnit.FAHRENHEIT.format(20));
    }
    @Test void temperaturesRespectItemMaximumAndColdFloor() {
        assertEquals(20, HeatTemperature.celsius(0, 1100), 0.001);
        assertEquals(1100, HeatTemperature.celsius(1, 1100), 0.001);
        assertEquals(1250, HeatTemperature.celsius(1, 1250), 0.001);
        assertEquals(560, HeatTemperature.celsius(0.5F, 1100), 0.001);
        assertEquals(20, HeatTemperature.celsius(-1, 1100), 0.001);
        assertEquals(1100, HeatTemperature.celsius(2, 1100), 0.001);
    }
    @Test void countdownEndsAtForgeThresholdRatherThanCold() {
        assertEquals(45, HeatTemperature.workableSeconds(1200, 1200));
        assertEquals(28, HeatTemperature.workableSeconds(860, 1200));
        assertEquals(0, HeatTemperature.workableSeconds(300, 1200));
        assertEquals(0, HeatTemperature.workableSeconds(200, 1200));
        assertEquals(1, HeatTemperature.workableSeconds(301, 1200));
        assertEquals(0, HeatTemperature.workableSeconds(301, 1201));
    }
    @Test void itemTemperatureOverridesAreValidated() {
        assertTrue(HeatTemperature.validOverride("other_mod:hot_metal=1450"));
        assertTrue(HeatTemperature.validOverride("minecraft:copper_ingot = 1100"));
        for (Object entry : new Object[]{"bad item=1100", "minecraft:copper_ingot=20",
                "minecraft:copper_ingot=5001", "minecraft:copper_ingot=NaN", "minecraft:copper_ingot", 7})
            assertFalse(HeatTemperature.validOverride(entry));
    }
}
