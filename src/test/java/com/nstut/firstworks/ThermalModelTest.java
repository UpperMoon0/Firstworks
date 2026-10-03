package com.nstut.firstworks;
import com.nstut.firstworks.content.workshop.ThermalModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ThermalModelTest {
    @Test void heatingIsGradualAndBounded() {
        assertEquals(30,ThermalModel.approach(20,800,10));
        assertEquals(800,ThermalModel.approach(799,800,10));
        assertEquals(800,ThermalModel.approach(1150,800,10));
    }
    @Test void boostHoldsThenDecaysWithoutStacking() {
        assertEquals(1150,ThermalModel.boostedCeiling(800,1150,240,80));
        assertEquals(1150,ThermalModel.boostedCeiling(800,1150,80,80));
        assertEquals(975,ThermalModel.boostedCeiling(800,1150,40,80));
        assertEquals(800,ThermalModel.boostedCeiling(800,1150,0,80));
    }
    @Test void coolingAndWorkWindowUseTemperature() {
        assertEquals(1082,ThermalModel.cool(1100,20,0.9),0.001);
        assertEquals(20,ThermalModel.cool(100,1000,0.9),0.001);
        assertEquals(34,ThermalModel.workableSeconds(1100,500,0.9));
        assertEquals(0,ThermalModel.workableSeconds(499,500,0.9));
    }
}
