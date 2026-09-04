/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTemperatureDataCoreTemperatureAdjustmentTest {
    private static final float EPSILON = 1.0e-6F;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void appliesPositiveAndNegativeDeltasToEveryBodyPart() {
        PlayerTemperatureData data = dataWithParts(
                10.0F, 20.0F, 30.0F, 40.0F, 50.0F);

        assertTrue(data.applyUniformBodyTemperatureDelta(2.5F));
        assertTemperatures(data, 12.5F, 22.5F, 32.5F, 42.5F, 52.5F);
        assertEquals(25.5F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(0.0F, data.getPreviousCoreBodyTemp(), EPSILON);

        assertTrue(data.applyUniformBodyTemperatureDelta(-4.0F));
        assertTemperatures(data, 8.5F, 18.5F, 28.5F, 38.5F, 48.5F);
        assertEquals(21.5F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(0.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    @Test
    void zeroDeltaStillRefreshesTheCoreTemperature() {
        PlayerTemperatureData data = dataWithParts(
                4.0F, 8.0F, 12.0F, 16.0F, 20.0F);

        assertTrue(data.applyUniformBodyTemperatureDelta(0.0F));

        assertTemperatures(data, 4.0F, 8.0F, 12.0F, 16.0F, 20.0F);
        assertEquals(9.2F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(0.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    @Test
    void rejectsNonFiniteDeltasWithoutChangingState() {
        PlayerTemperatureData data = dataWithParts(
                10.0F, 20.0F, 30.0F, 40.0F, 50.0F);
        data.applyUniformBodyTemperatureDelta(0.0F);

        assertFalse(data.applyUniformBodyTemperatureDelta(Float.NaN));
        assertUnchanged(data, 10.0F, 20.0F, 30.0F, 40.0F, 50.0F, 23.0F);

        assertFalse(data.applyUniformBodyTemperatureDelta(
                Float.POSITIVE_INFINITY));
        assertUnchanged(data, 10.0F, 20.0F, 30.0F, 40.0F, 50.0F, 23.0F);
    }

    @Test
    void rejectsFiniteDeltasThatWouldOverflowAnyCorePart() {
        PlayerTemperatureData data = dataWithParts(
                Float.MAX_VALUE, 2.0F, 3.0F, 4.0F, 5.0F);
        data.applyUniformBodyTemperatureDelta(0.0F);
        float coreBefore = data.getCoreBodyTemp();

        assertFalse(data.applyUniformBodyTemperatureDelta(Float.MAX_VALUE));

        assertUnchanged(
                data, Float.MAX_VALUE, 2.0F, 3.0F, 4.0F, 5.0F, coreBefore);
    }

    private static PlayerTemperatureData dataWithParts(float head, float torso, float legs,
                                                       float hands, float feet) {
        PlayerTemperatureData data = new PlayerTemperatureData();
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HEAD, head);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.TORSO, torso);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.LEGS, legs);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS, hands);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.FEET, feet);

        data.refreshCoreTemperature();
        return data;
    }

    private static void assertUnchanged(PlayerTemperatureData data, float head, float torso, float legs,
                                        float hands, float feet, float core) {
        assertTemperatures(data, head, torso, legs, hands, feet);
        assertEquals(core, data.getCoreBodyTemp(), EPSILON);
        assertEquals(0.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    private static void assertTemperatures(PlayerTemperatureData data, float head, float torso, float legs,
                                           float hands, float feet) {
        assertEquals(head, data.getBodyTempByPart(PlayerTemperatureData.BodyPart.HEAD), EPSILON);
        assertEquals(torso, data.getBodyTempByPart(PlayerTemperatureData.BodyPart.TORSO), EPSILON);
        assertEquals(legs, data.getBodyTempByPart(PlayerTemperatureData.BodyPart.LEGS), EPSILON);
        assertEquals(hands, data.getBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS), EPSILON);
        assertEquals(feet, data.getBodyTempByPart(PlayerTemperatureData.BodyPart.FEET), EPSILON);
    }
}
