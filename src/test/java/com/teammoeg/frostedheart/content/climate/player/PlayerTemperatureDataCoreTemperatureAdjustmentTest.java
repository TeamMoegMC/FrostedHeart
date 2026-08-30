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
import net.minecraft.nbt.CompoundTag;
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
    void appliesPositiveAndNegativeDeltasToOnlyCoreParts() {
        PlayerTemperatureData data = dataWithParts(10.0F, 20.0F, 30.0F, 40.0F, 50.0F, -3.0F);

        assertTrue(data.applyCoreBodyTemperatureDelta(2.5F));
        assertTemperatures(data, 12.5F, 22.5F, 32.5F, 40.0F, 50.0F);
        assertEquals(25.5F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(-3.0F, data.getPreviousCoreBodyTemp(), EPSILON);

        assertTrue(data.applyCoreBodyTemperatureDelta(-4.0F));
        assertTemperatures(data, 8.5F, 18.5F, 28.5F, 40.0F, 50.0F);
        assertEquals(21.5F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(-3.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    @Test
    void zeroDeltaStillRefreshesTheCoreTemperature() {
        PlayerTemperatureData data = dataWithParts(4.0F, 8.0F, 12.0F, 16.0F, 20.0F, -2.0F);

        assertTrue(data.applyCoreBodyTemperatureDelta(0.0F));

        assertTemperatures(data, 4.0F, 8.0F, 12.0F, 16.0F, 20.0F);
        assertEquals(9.2F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(-2.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    @Test
    void rejectsNonFiniteDeltasWithoutChangingState() {
        PlayerTemperatureData data = dataWithParts(10.0F, 20.0F, 30.0F, 40.0F, 50.0F, 6.0F);
        data.applyCoreBodyTemperatureDelta(0.0F);

        assertFalse(data.applyCoreBodyTemperatureDelta(Float.NaN));
        assertUnchanged(data, 10.0F, 20.0F, 30.0F, 40.0F, 50.0F, 23.0F, 6.0F);

        assertFalse(data.applyCoreBodyTemperatureDelta(Float.POSITIVE_INFINITY));
        assertUnchanged(data, 10.0F, 20.0F, 30.0F, 40.0F, 50.0F, 23.0F, 6.0F);
    }

    @Test
    void normalUpdateUsesTheSameCoreRecalculationAndAdvancesPreviousOnlyOnce() {
        PlayerTemperatureData data = dataWithParts(10.0F, 20.0F, 30.0F, 40.0F, 50.0F, -3.0F);
        data.applyCoreBodyTemperatureDelta(0.0F);
        HeatingDeviceContext context = new HeatingDeviceContext(null);
        context.setPartData(PlayerTemperatureData.BodyPart.HEAD, 1.0F, 1.0F);
        context.setPartData(PlayerTemperatureData.BodyPart.TORSO, 2.0F, 2.0F);
        context.setPartData(PlayerTemperatureData.BodyPart.LEGS, 3.0F, 3.0F);
        context.setPartData(PlayerTemperatureData.BodyPart.HANDS, 4.0F, 4.0F);
        context.setPartData(PlayerTemperatureData.BodyPart.FEET, 5.0F, 5.0F);

        data.update(0.0F, context, 0.0F);

        assertTemperatures(data, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F);
        assertEquals(2.3F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(23.0F, data.getPreviousCoreBodyTemp(), EPSILON);

        assertTrue(data.applyCoreBodyTemperatureDelta(1.0F));
        assertTemperatures(data, 2.0F, 3.0F, 4.0F, 4.0F, 5.0F);
        assertEquals(3.3F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(23.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    @Test
    void rejectsFiniteDeltasThatWouldOverflowAnyCorePart() {
        PlayerTemperatureData data = dataWithParts(Float.MAX_VALUE, 2.0F, 3.0F, 4.0F, 5.0F, 7.0F);
        data.applyCoreBodyTemperatureDelta(0.0F);
        float coreBefore = data.getCoreBodyTemp();

        assertFalse(data.applyCoreBodyTemperatureDelta(Float.MAX_VALUE));

        assertUnchanged(data, Float.MAX_VALUE, 2.0F, 3.0F, 4.0F, 5.0F, coreBefore, 7.0F);
    }

    private static PlayerTemperatureData dataWithParts(float head, float torso, float legs,
                                                       float hands, float feet, float previousCore) {
        PlayerTemperatureData data = new PlayerTemperatureData();
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HEAD, head);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.TORSO, torso);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.LEGS, legs);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS, hands);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.FEET, feet);

        CompoundTag tag = new CompoundTag();
        tag.putFloat("previous_body_temperature", previousCore);
        data.load(tag, true);
        return data;
    }

    private static void assertUnchanged(PlayerTemperatureData data, float head, float torso, float legs,
                                        float hands, float feet, float core, float previousCore) {
        assertTemperatures(data, head, torso, legs, hands, feet);
        assertEquals(core, data.getCoreBodyTemp(), EPSILON);
        assertEquals(previousCore, data.getPreviousCoreBodyTemp(), EPSILON);
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
