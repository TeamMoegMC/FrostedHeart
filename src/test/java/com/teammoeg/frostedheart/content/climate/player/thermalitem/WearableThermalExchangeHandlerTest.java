/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WearableThermalExchangeHandlerTest {
    private static final float EPSILON = 1.0e-6F;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void initializedReservoirExchangesBothDirectionsWithOneWrite() {
        assertExchangeDirection(60.0D, true);
        assertExchangeDirection(0.0D, false);
    }

    @Test
    void missingStateInitializesOnceWithoutAdvancingEitherSide() {
        PlayerTemperatureData playerData = playerData();
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        WearableThermalExchangeHandler handler = new WearableThermalExchangeHandler();

        WearableThermalExchangeHandler.Status status = handler.exchangeInto(
                playerData, stack, reservoir, -12.5D, 1.0D);

        assertEquals(WearableThermalExchangeHandler.Status.INITIALIZED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(1, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertEquals(-12.5D, state.coreTemperatureC());
        assertEquals(-12.5D, state.surfaceTemperatureC());
        assertPlayerUnchanged(playerData);
    }

    @Test
    void unavailableEnvironmentLeavesMissingStateAndPlayerUntouched() {
        PlayerTemperatureData playerData = playerData();
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();

        WearableThermalExchangeHandler.Status status =
                new WearableThermalExchangeHandler().exchangeInto(
                        playerData, stack, reservoir, Double.NaN, 1.0D);

        assertEquals(WearableThermalExchangeHandler.Status.STATE_UNAVAILABLE, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(1, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        assertFalse(stack.hasTag());
        assertPlayerUnchanged(playerData);
    }

    @Test
    void equalTemperaturesDoNotWriteOrMoveThePlayer() {
        PlayerTemperatureData playerData = playerData();
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(37.0D, 37.0D).writeTo(stack);
        CompoundTag before = stack.getTag().copy();

        WearableThermalExchangeHandler.Status status =
                new WearableThermalExchangeHandler().exchangeInto(
                        playerData, stack, reservoir, 5.0D, 20.0D);

        assertEquals(WearableThermalExchangeHandler.Status.UNCHANGED, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(0, reservoir.setCount);
        assertEquals(before, stack.getTag());
        assertPlayerUnchanged(playerData);
    }

    @Test
    void degradedElapsedTimeIsAnAtomicNoOp() {
        PlayerTemperatureData playerData = playerData();
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(60.0D, 20.0D).writeTo(stack);
        CompoundTag before = stack.getTag().copy();

        WearableThermalExchangeHandler.Status status =
                new WearableThermalExchangeHandler().exchangeInto(
                        playerData, stack, reservoir, 5.0D, Double.NaN);

        assertEquals(WearableThermalExchangeHandler.Status.NUMERIC_DEGRADED, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(0, reservoir.setCount);
        assertEquals(before, stack.getTag());
        assertPlayerUnchanged(playerData);
    }

    @Test
    void managementModesSkipBeforeGameplayWork() {
        assertTrue(WearableThermalExchangeHandler.allowsPlayerExchange(
                false, false, false));
        assertFalse(WearableThermalExchangeHandler.allowsPlayerExchange(
                true, false, false));
        assertFalse(WearableThermalExchangeHandler.allowsPlayerExchange(
                false, true, false));
        assertFalse(WearableThermalExchangeHandler.allowsPlayerExchange(
                false, false, true));
    }

    @Test
    void nonReservoirStackIsIgnored() {
        PlayerTemperatureData playerData = playerData();
        WearableThermalExchangeHandler.Status status =
                new WearableThermalExchangeHandler().exchangeInto(
                        playerData, stack(), 0.0D, 1.0D);

        assertEquals(WearableThermalExchangeHandler.Status.NOT_WEARABLE, status);
        assertEquals(0, status.stackWriteCount());
        assertPlayerUnchanged(playerData);
    }

    private static void assertExchangeDirection(
            double initialReservoirTemperatureC,
            boolean warmsPlayer
    ) {
        PlayerTemperatureData playerData = playerData();
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(
                initialReservoirTemperatureC,
                initialReservoirTemperatureC).writeTo(stack);

        WearableThermalExchangeHandler.Status status =
                new WearableThermalExchangeHandler().exchangeInto(
                        playerData, stack, reservoir, 5.0D, 20.0D);

        assertEquals(WearableThermalExchangeHandler.Status.APPLIED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(1, reservoir.setCount);
        assertEquals(0, reservoir.restoreCount);
        if (warmsPlayer) {
            assertTrue(playerData.getCoreBodyTemp() > 0.0F);
        } else {
            assertTrue(playerData.getCoreBodyTemp() < 0.0F);
        }
        assertEquals(-7.0F, playerData.getPreviousCoreBodyTemp(), EPSILON);
        assertEquals(-4.0F,
                playerData.getBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS),
                EPSILON);
        assertEquals(-5.0F,
                playerData.getBodyTempByPart(PlayerTemperatureData.BodyPart.FEET),
                EPSILON);
        WearableThermalState next = WearableThermalState.read(stack).orElseThrow();
        if (warmsPlayer) {
            assertTrue(next.surfaceTemperatureC() < initialReservoirTemperatureC);
        } else {
            assertTrue(next.surfaceTemperatureC() > initialReservoirTemperatureC);
        }
    }

    private static PlayerTemperatureData playerData() {
        PlayerTemperatureData data = new PlayerTemperatureData();
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HEAD, 0.0F);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.TORSO, 0.0F);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.LEGS, 0.0F);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS, -4.0F);
        data.setBodyTempByPart(PlayerTemperatureData.BodyPart.FEET, -5.0F);
        CompoundTag packet = new CompoundTag();
        packet.putFloat("previous_body_temperature", -7.0F);
        data.load(packet, true);
        data.applyCoreBodyTemperatureDelta(0.0F);
        return data;
    }

    private static void assertPlayerUnchanged(PlayerTemperatureData data) {
        assertEquals(0.0F, data.getCoreBodyTemp(), EPSILON);
        assertEquals(0.0F,
                data.getBodyTempByPart(PlayerTemperatureData.BodyPart.HEAD), EPSILON);
        assertEquals(0.0F,
                data.getBodyTempByPart(PlayerTemperatureData.BodyPart.TORSO), EPSILON);
        assertEquals(0.0F,
                data.getBodyTempByPart(PlayerTemperatureData.BodyPart.LEGS), EPSILON);
        assertEquals(-4.0F,
                data.getBodyTempByPart(PlayerTemperatureData.BodyPart.HANDS), EPSILON);
        assertEquals(-5.0F,
                data.getBodyTempByPart(PlayerTemperatureData.BodyPart.FEET), EPSILON);
        assertEquals(-7.0F, data.getPreviousCoreBodyTemp(), EPSILON);
    }

    private static ItemStack stack() {
        return new ItemStack(Items.STONE);
    }

    private static final class CountingReservoir implements WearableThermalReservoir {
        private int restoreCount;
        private int setCount;

        @Override
        public WearableThermalProfile thermalProfile(ItemStack stack) {
            return WearableThermalProfile.WARM_STONE_DEFAULT;
        }

        @Override
        public Optional<WearableThermalState> restoreOrInitializeThermalStateForServer(
                ItemStack stack,
                double serverEnvironmentTemperatureC
        ) {
            restoreCount++;
            return WearableThermalReservoir.super
                    .restoreOrInitializeThermalStateForServer(
                            stack, serverEnvironmentTemperatureC);
        }

        @Override
        public void setTemperaturesC(
                ItemStack stack,
                double coreTemperatureC,
                double surfaceTemperatureC
        ) {
            setCount++;
            WearableThermalReservoir.super.setTemperaturesC(
                    stack, coreTemperatureC, surfaceTemperatureC);
        }
    }
}
