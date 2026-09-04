/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.DimensionInputAccumulator;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicalSourceSpatialIndexTest {
    @Test
    void sourceRejectedAtCapacityCanEnterAfterARelease() {
        DimensionInputAccumulator accumulator =
                new DimensionInputAccumulator(1L, 0L);
        PhysicalSourceSpatialIndex sources =
                new PhysicalSourceSpatialIndex(
                        accumulator, null,
                        MinecraftPhysicalSourceProfile.CAMPFIRE,
                        1, 1);
        BlockPos first = BlockPos.ZERO;
        BlockPos second = new BlockPos(1, 0, 0);

        sources.observeMachine(
                first, first, MinecraftPhysicalSourceProfile.GENERATOR,
                1.0D, true);
        sources.observeMachine(
                second, second, MinecraftPhysicalSourceProfile.GENERATOR,
                1.0D, true);
        assertTrue(sources.capacityRecoveryPending());

        sources.remove(first.getX(), first.getY(), first.getZ());
        sources.flush(20L);
        assertTrue(sources.hasAvailableCapacity());

        sources.beginCapacityRecoveryPass();
        sources.observeMachine(
                second, second, MinecraftPhysicalSourceProfile.GENERATOR,
                1.0D, true);
        assertFalse(sources.hasAvailableCapacity());
        assertFalse(sources.capacityRecoveryPending());
        sources.close();
    }
}
