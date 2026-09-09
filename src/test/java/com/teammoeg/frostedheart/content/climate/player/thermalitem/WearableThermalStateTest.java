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

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WearableThermalStateTest {
    private static final double EPSILON = 1.0e-6D;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void missingStateInitializesBothNodesFromTheFiniteServerEnvironment() {
        ItemStack stack = stack();

        WearableThermalState state = WearableThermalState
                .restoreOrInitializeForServer(stack, 23.25D)
                .orElseThrow();

        assertEquals(23.25D, state.coreTemperatureC(), EPSILON);
        assertEquals(23.25D, state.surfaceTemperatureC(), EPSILON);
        CompoundTag reservoir = stack.getTag().getCompound(WearableThermalState.ROOT_KEY);
        assertEquals(WearableThermalState.SCHEMA_VERSION,
                reservoir.getInt(WearableThermalState.VERSION_KEY));
        assertTrue(reservoir.getBoolean(WearableThermalState.INITIALIZED_KEY));
        assertEquals(Set.of(
                        WearableThermalState.VERSION_KEY,
                        WearableThermalState.INITIALIZED_KEY,
                        WearableThermalState.CORE_TEMPERATURE_KEY,
                        WearableThermalState.SURFACE_TEMPERATURE_KEY),
                reservoir.getAllKeys());
    }

    @Test
    void versionOneRoundTripPreservesBothIndependentTemperatures() {
        ItemStack stack = stack();
        new WearableThermalState(62.5D, 14.25D).writeTo(stack);

        WearableThermalState decoded = WearableThermalState.read(stack).orElseThrow();

        assertEquals(62.5D, decoded.coreTemperatureC(), EPSILON);
        assertEquals(14.25D, decoded.surfaceTemperatureC(), EPSILON);
    }

    @Test
    void inclusiveTemperatureBoundariesAreValid() {
        ItemStack stack = stack();
        new WearableThermalState(-1_000.0D, 1_000.0D).writeTo(stack);

        WearableThermalState decoded = WearableThermalState.read(stack).orElseThrow();

        assertEquals(-1_000.0D, decoded.coreTemperatureC());
        assertEquals(1_000.0D, decoded.surfaceTemperatureC());
    }

    @Test
    void unknownSchemaAndInvalidTemperaturesRepairOnlyWithFiniteEnvironment() {
        ItemStack unknownSchema = stack();
        CompoundTag unknown = new CompoundTag();
        unknown.putInt(WearableThermalState.VERSION_KEY, 2);
        unknown.putBoolean(WearableThermalState.INITIALIZED_KEY, true);
        unknown.putFloat(WearableThermalState.CORE_TEMPERATURE_KEY, 30.0F);
        unknown.putFloat(WearableThermalState.SURFACE_TEMPERATURE_KEY, 30.0F);
        unknownSchema.getOrCreateTag().put(WearableThermalState.ROOT_KEY, unknown);

        WearableThermalState repaired = WearableThermalState
                .restoreOrInitializeForServer(unknownSchema, 1_500.0D)
                .orElseThrow();
        assertEquals(1_000.0D, repaired.coreTemperatureC());
        assertEquals(1_000.0D, repaired.surfaceTemperatureC());

        ItemStack invalidTemperature = stack();
        CompoundTag invalid = new CompoundTag();
        invalid.putInt(WearableThermalState.VERSION_KEY, WearableThermalState.SCHEMA_VERSION);
        invalid.putBoolean(WearableThermalState.INITIALIZED_KEY, true);
        invalid.putDouble(WearableThermalState.CORE_TEMPERATURE_KEY, Double.NaN);
        invalid.putFloat(WearableThermalState.SURFACE_TEMPERATURE_KEY, 20.0F);
        invalidTemperature.getOrCreateTag().put(WearableThermalState.ROOT_KEY, invalid);

        WearableThermalState repairedInvalid = WearableThermalState
                .restoreOrInitializeForServer(invalidTemperature, -1_500.0D)
                .orElseThrow();
        assertEquals(-1_000.0D, repairedInvalid.coreTemperatureC());
        assertEquals(-1_000.0D, repairedInvalid.surfaceTemperatureC());
    }

    @Test
    void unavailableEnvironmentLeavesUninitializedOrInvalidStateUntouched() {
        ItemStack missing = stack();
        assertFalse(WearableThermalState.restoreOrInitializeForServer(missing, Double.NaN)
                .isPresent());
        assertFalse(missing.hasTag());

        ItemStack invalid = stack();
        CompoundTag root = invalid.getOrCreateTag();
        root.put(WearableThermalState.ROOT_KEY, new CompoundTag());
        CompoundTag before = root.copy();

        assertFalse(WearableThermalState.restoreOrInitializeForServer(
                invalid, Double.NEGATIVE_INFINITY).isPresent());
        assertEquals(before, invalid.getTag());
    }

    @Test
    void invalidSourceStatesAreRejectedBeforeTheyCanBeWritten() {
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalState(Double.NaN, 10.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new WearableThermalState(-1_000.1D, 10.0D));
    }

    private static ItemStack stack() {
        return new ItemStack(Items.STONE);
    }
}
