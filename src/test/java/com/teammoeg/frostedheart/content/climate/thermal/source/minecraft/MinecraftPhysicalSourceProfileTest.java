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
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MinecraftPhysicalSourceProfileTest {
    @Test
    void campfireFreezesOneKilowattWithExplicitBlockedConvectionLoss() {
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.CAMPFIRE;
        MinecraftPhysicalSourceProfile.Port[] ports = profile.ports();

        assertEquals(1_000.0D, profile.ratedPowerW());
        assertEquals(
                MinecraftPhysicalSourceProfile.MissingPortPolicy.EXPLICIT_LOSS,
                profile.missingPortPolicy());
        assertEquals(2, ports.length);
        assertEquals(0.8D, ports[0].powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.AIR_FACE, ports[0].kind());
        assertEquals(1, ports[0].offsetY());
        assertEquals(ConservativeAirGeometry.Face.NEGATIVE_Y, ports[0].targetFace());
        assertEquals(0.2D, ports[1].powerShare());
        assertEquals(SourceChannel.RADIATION, ports[1].channel());
    }

    @Test
    void generatorScalesTenKilowattsPerThermalLevelAcrossThreeSinks() {
        MinecraftPhysicalSourceProfile profile =
                MinecraftPhysicalSourceProfile.GENERATOR;
        MinecraftPhysicalSourceProfile.Port[] ports = profile.ports();

        assertEquals(20_000.0D, profile.powerForLevel(2.0D));
        assertEquals(
                MinecraftPhysicalSourceProfile.MissingPortPolicy.INTERNAL_HEAT,
                profile.missingPortPolicy());
        assertEquals(3, ports.length);
        assertEquals(0.7D, ports[0].powerShare());
        assertEquals(0.1D, ports[1].powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.INTERNAL_HEAT, ports[1].kind());
        assertEquals(0.2D, ports[2].powerShare());
        assertEquals(MinecraftPhysicalSourceProfile.PortKind.DECLARED_LOSS, ports[2].kind());
    }

    @Test
    void profileRejectsAnIncompletePowerPartition() {
        assertThrows(IllegalArgumentException.class, () ->
                new MinecraftPhysicalSourceProfile(
                        3,
                        100.0D,
                        MinecraftPhysicalSourceProfile.MissingPortPolicy.EXPLICIT_LOSS,
                        new MinecraftPhysicalSourceProfile.Port[]{
                                MinecraftPhysicalSourceProfile.Port.declaredLoss(
                                        0, SourceChannel.RADIATION, 0.5D)
                        }));
    }
}
