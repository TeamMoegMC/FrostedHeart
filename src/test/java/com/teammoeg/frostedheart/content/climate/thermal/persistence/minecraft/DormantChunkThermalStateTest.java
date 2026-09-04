/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;

import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DormantChunkThermalStateTest {
    @Test
    void unsupportedResidualUsesConfiguredHalfLife() {
        DormantChunkThermalState state = new DormantChunkThermalState(0, 1);
        state.replace(0, entry(false, (short) 160));

        ThermalInputBatch.DormantAirCut cut = state.admissionCut(
                0, 600L, 30.0D, 0.0D);

        assertNotNull(cut);
        assertEquals(5.0D, cut.meanTemperatureC(0), 1.0e-9D);
    }

    @Test
    void mixedRestoreKeepsExactValuesAndFallsBackToStoredMean() {
        DormantChunkThermalState state = new DormantChunkThermalState(0, 1);
        state.replace(0, mixedEntry(false));
        ThermalInputBatch.DormantAirCut cut = state.admissionCut(
                0, 0L, 30.0D, 0.0D);

        assertNotNull(cut);
        assertEquals(10.0D, cut.componentTemperatureC(0, 0, 2), 1.0e-9D);
        assertEquals(-2.0D, cut.componentTemperatureC(0, 1, 2), 1.0e-9D);
        assertEquals(4.0D, cut.componentTemperatureC(0, 0, 3), 1.0e-9D);
        assertEquals(1L, state.storedBrickMask(0));
        assertEquals(4.0D, state.brickMeanTemperatureC(
                0, 0, 0L, 30.0D, 0.0D), 1.0e-9D);
    }

    @Test
    void diskSupportIsConsumedOnceAndSurvivesNbtRoundTrip() {
        DormantChunkThermalState state = new DormantChunkThermalState(0, 1);
        state.replace(0, mixedEntry(true));

        assertTrue(state.activateLoaded(600L, 30.0D));
        assertTrue(state.sourceSupported(0));
        ThermalInputBatch.DormantAirCut activated = state.admissionCut(
                0, 600L, 30.0D, 0.0D);
        assertNotNull(activated);
        assertEquals(4.0D, activated.meanTemperatureC(0), 1.0e-9D);
        assertEquals(-2.0D, activated.componentTemperatureC(0, 1, 2), 1.0e-9D);

        CompoundTag chunk = new CompoundTag();
        state.encode(chunk);
        DormantChunkThermalState decoded = DormantChunkThermalState.decode(
                chunk, 0, 1);
        assertNotNull(decoded);
        assertFalse(decoded.activateLoaded(600L, 30.0D));
        assertFalse(decoded.sourceSupported(0));
    }

    @Test
    void sourceSupportAndBrickMeanFallbackSurviveNbtRoundTrip() {
        DormantChunkThermalState state = new DormantChunkThermalState(0, 1);
        state.replace(0, entry(false, (short) 160));

        assertTrue(state.updateSourceSupport(0, true));
        assertEquals(1L, state.storedBrickMask(0));
        assertEquals(10.0D, state.brickMeanTemperatureC(
                0, 0, 0L, 30.0D, 0.0D), 1.0e-9D);

        CompoundTag chunk = new CompoundTag();
        state.encode(chunk);
        DormantChunkThermalState decoded = DormantChunkThermalState.decode(
                chunk, 0, 1);
        assertNotNull(decoded);
        assertEquals(1L, decoded.storedBrickMask(0));
        assertTrue(decoded.activateLoaded(600L, 30.0D));
        assertEquals(10.0D, decoded.brickMeanTemperatureC(
                0, 0, 600L, 30.0D, 0.0D), 1.0e-9D);
        assertTrue(decoded.sourceSupported(0));
        assertFalse(decoded.updateSourceSupport(0, false));
        assertFalse(decoded.sourceSupported(0));
    }

    private static DormantChunkThermalState.SectionEntry entry(
            boolean supported,
            short residual
    ) {
        return new DormantChunkThermalState.SectionEntry(
                0L, supported, 1L, 0L, new byte[0], packed(residual));
    }

    private static DormantChunkThermalState.SectionEntry mixedEntry(
            boolean supported
    ) {
        return new DormantChunkThermalState.SectionEntry(
                0L,
                supported,
                1L,
                1L,
                new byte[]{1},
                packed((short) 64, (short) 160, (short) -32));
    }

    private static long[] packed(short... values) {
        long[] result = new long[(values.length + 3) >>> 2];
        for (int index = 0; index < values.length; index++) {
            result[index >>> 2] |= ((long) values[index] & 0xffffL)
                    << ((index & 3) << 4);
        }
        return result;
    }
}
