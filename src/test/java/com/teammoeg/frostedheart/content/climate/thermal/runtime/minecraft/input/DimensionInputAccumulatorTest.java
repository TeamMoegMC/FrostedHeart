/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionInputAccumulatorTest {
    @Test
    void sequenceAdvancesAcrossCutsAndRestartsWithANewGeneration() {
        DimensionInputAccumulator firstGeneration =
                new DimensionInputAccumulator(1L, 0L);

        ThermalInputBatch first = firstGeneration.seal(20L);
        ThermalInputBatch second = firstGeneration.seal(40L);
        ThermalInputBatch restarted =
                new DimensionInputAccumulator(2L, 40L).seal(60L);

        assertEquals(1L, first.sequence());
        assertEquals(2L, second.sequence());
        assertEquals(1L, restarted.sequence());
        assertEquals(2L, restarted.dimensionGeneration());
    }

    @Test
    void environmentUpdatesCoalesceByPageAndColumnBeforeSeal() {
        DimensionInputAccumulator accumulator =
                new DimensionInputAccumulator(1L, 0L);
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        accumulator.updateNaturalTemperature(page, 5.0D);
        accumulator.updateNaturalTemperature(page, 6.0D);
        accumulator.updateSkyColumn(page, 4, 10);
        accumulator.updateSkyColumn(page, 4, 12);

        ThermalInputBatch batch = accumulator.seal(20L);

        assertEquals(1, batch.environmentUpdates().length);
        ThermalInputBatch.PageEnvironmentUpdate update =
                batch.environmentUpdates()[0];
        assertEquals(6.0D, update.naturalTemperatureC());
        assertEquals(1, update.skyColumns().length);
        assertEquals(4, update.skyColumns()[0]);
        assertEquals(12, update.firstExposedLocalY()[0]);
    }

    @Test
    void unsealedAdmissionIsCancelledWhenItsPageRetires() {
        DimensionInputAccumulator accumulator =
                new DimensionInputAccumulator(1L, 0L);
        ThermalPageHandle page = new ThermalPageHandle(0L, 1L);
        accumulator.admit(
                page,
                0L,
                ThermalTestFixtures.filledPageSignatures(0),
                0.0D,
                new byte[256],
                null);
        accumulator.retire(page);

        ThermalInputBatch batch = accumulator.seal(20L);

        assertEquals(0, batch.admissions().length);
        assertEquals(0, batch.retirements().length);
    }
}
