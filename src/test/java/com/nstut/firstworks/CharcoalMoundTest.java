package com.nstut.firstworks;

import com.nstut.firstworks.content.charcoal.CharcoalMoundData;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CharcoalMoundTest {

    @Test
    public void testPhaseEnumValues() {
        assertNotNull(CharcoalMoundData.Phase.valueOf("WAITING_FOR_SEAL"));
        assertNotNull(CharcoalMoundData.Phase.valueOf("CARBONIZING"));
        assertNotNull(CharcoalMoundData.Phase.valueOf("LEGACY_READY"));
    }

    @Test
    public void testMoundStatusRecord() {
        CharcoalMoundData.MoundStatus status = new CharcoalMoundData.MoundStatus(
                CharcoalMoundData.Phase.CARBONIZING, 16, 2400L, 12);
        assertEquals(CharcoalMoundData.Phase.CARBONIZING, status.phase());
        assertEquals(16, status.logCount());
        assertEquals(2400L, status.remainingTicks());
        assertEquals(12, status.expectedYield());
    }

    @Test
    public void testIgnitionResultRecord() {
        CharcoalMoundData.IgnitionResult success = CharcoalMoundData.IgnitionResult.success();
        assertTrue(success.isSuccessful());
        assertNull(success.message());

        CharcoalMoundData.IgnitionResult failure = CharcoalMoundData.IgnitionResult.failure(null);
        assertFalse(failure.isSuccessful());
    }
}
