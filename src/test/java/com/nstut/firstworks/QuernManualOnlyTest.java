package com.nstut.firstworks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class QuernManualOnlyTest {
    private static final Path QUERN_PACKAGE = Path.of("src/main/java/com/nstut/firstworks/content/quern");

    @Test
    public void quernHasNoExternalPowerApi() throws Exception {
        assertFalse(Files.exists(QUERN_PACKAGE.resolve("QuernDriveable.java")));

        String entity = Files.readString(QUERN_PACKAGE.resolve("QuernBlockEntity.java"));
        assertFalse(entity.contains("QuernDriveable"));
        assertFalse(entity.contains("driveRate"));
        assertFalse(entity.contains("setDriven"));
        assertFalse(entity.contains("setDriveRate"));
        assertFalse(entity.contains("getDriveRate"));
    }

    @Test
    public void quernOnlyTicksForClientAnimation() throws Exception {
        String block = Files.readString(QUERN_PACKAGE.resolve("QuernBlock.java"));
        assertTrue(block.contains("if (!level.isClientSide) return null;"));
        assertTrue(block.contains("QuernBlockEntity::clientTick"));
        assertFalse(block.contains("QuernBlockEntity::tick"));
    }

    @Test
    public void onlyManualWorkRateIsConfigurable() throws Exception {
        String config = Files.readString(Path.of("src/main/java/com/nstut/firstworks/FirstworksConfig.java"));
        assertTrue(config.contains("quernManualWorkPerCrank"));
        assertFalse(config.contains("quernDefaultDrivenWorkPerTick"));
    }

    @Test
    public void crankAnimationUsesMonotonicForwardSteps() throws Exception {
        String entity = Files.readString(QUERN_PACKAGE.resolve("QuernBlockEntity.java"));
        // Rotation still advances monotonically forward (here scaled by the work applied per crank, so a
        // higher work rate reads as a faster spin without reversing).
        assertTrue(entity.contains("rotationSteps += workAmount"));
        assertTrue(entity.contains("putLong(\"RotationSteps\", rotationSteps)"));
        assertTrue(entity.contains("rotationTarget = rotationSteps * 45D"));
        assertFalse(entity.contains("while (diff"), "Animation must not select a shortest path that can reverse");
        assertFalse(entity.contains("rotation = (rotation + 45F) % 360F"));
    }
}
