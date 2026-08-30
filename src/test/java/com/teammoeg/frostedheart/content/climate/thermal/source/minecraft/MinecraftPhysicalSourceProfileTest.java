/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftPhysicalSourceProfileTest {
    @Test
    void campfireUsesEightKilowattsWithExplicitBlockedConvectionLoss() {
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.CAMPFIRE;

        assertEquals(8_000.0D, profile.powerForLevel(1.0D));
        assertEquals(
                MinecraftPhysicalSourceProfile.MissingPortPolicy.EXPLICIT_LOSS,
                profile.missingPortPolicy());
        assertEquals(2, profile.portCount());
        assertEquals(0.8D, profile.port(0).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.AIR_FACE,
                profile.port(0).kind());
        assertEquals(1, profile.port(0).offsetY());
        assertEquals(ConservativeAirGeometry.Face.NEGATIVE_Y,
                profile.port(0).targetFace());
        assertEquals(0.2D, profile.port(1).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.RADIATION_LOSS,
                profile.port(1).kind());
        assertEquals(1_600.0D, profile.radiativePowerW(8_000.0D));
    }

    @Test
    void generatorScalesTenKilowattsPerThermalLevelAcrossThreeSinks() {
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.GENERATOR;

        assertEquals(20_000.0D, profile.powerForLevel(2.0D));
        assertEquals(
                MinecraftPhysicalSourceProfile.MissingPortPolicy.INTERNAL_HEAT,
                profile.missingPortPolicy());
        assertEquals(3, profile.portCount());
        assertEquals(0.7D, profile.port(0).powerShare());
        assertEquals(0.1D, profile.port(1).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.INTERNAL_HEAT,
                profile.port(1).kind());
        assertEquals(0.2D, profile.port(2).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.RADIATION_LOSS,
                profile.port(2).kind());
    }

    @Test
    void fountainAndRadiatorUseOnlyPhysicalPowerPartitions() {
        MinecraftPhysicalSourceProfile fountain =
                MinecraftPhysicalSourceProfile.FOUNTAIN;
        assertEquals(4_000.0D, fountain.powerForLevel(2.0D));
        assertEquals(0.9D, fountain.port(0).powerShare());
        assertEquals(0.1D, fountain.port(1).powerShare());

        MinecraftPhysicalSourceProfile radiator =
                MinecraftPhysicalSourceProfile.RADIATOR;
        assertEquals(8_000.0D, radiator.powerForLevel(2.0D));
        assertEquals(0.8D, radiator.port(0).powerShare());
        assertEquals(0.1D, radiator.port(1).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.INTERNAL_HEAT,
                radiator.port(1).kind());
        assertEquals(0.1D, radiator.port(2).powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.RADIATION_LOSS,
                radiator.port(2).kind());
    }

    @Test
    void profileRejectsAnIncompletePowerPartition() {
        assertThrows(IllegalArgumentException.class, () ->
                new MinecraftPhysicalSourceProfile(
                        3,
                        100.0D,
                        MinecraftPhysicalSourceProfile.MissingPortPolicy.EXPLICIT_LOSS,
                        new MinecraftPhysicalSourceProfile.Port[]{
                                MinecraftPhysicalSourceProfile.Port.radiationLoss(
                                        0, 0.5D)
                        },
                        0.5D, 0.5D, 0.5D, 1.0D));
    }
}
