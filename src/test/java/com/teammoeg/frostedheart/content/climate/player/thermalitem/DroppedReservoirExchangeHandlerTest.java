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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedReservoirExchangeHandlerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void stableIdentityStaggersTwentyTickLoadedCadence() {
        Set<Integer> buckets = new HashSet<>();
        for (int index = 0; index < 100; index++) {
            UUID identity = new UUID(index * 31L, index * 17L + 1L);
            int bucket = DroppedReservoirExchangeHandler.cadenceBucket(identity);
            buckets.add(bucket);
            assertTrue(bucket >= 0
                    && bucket < DroppedReservoirExchangeHandler.CADENCE_TICKS);
            assertFalse(DroppedReservoirExchangeHandler.isCadenceTick(
                    identity, DroppedReservoirExchangeHandler.CADENCE_TICKS - 1));
            assertTrue(DroppedReservoirExchangeHandler.isCadenceTick(
                    identity,
                    DroppedReservoirExchangeHandler.CADENCE_TICKS + bucket));
            assertTrue(DroppedReservoirExchangeHandler.isCadenceTick(
                    identity,
                    DroppedReservoirExchangeHandler.CADENCE_TICKS * 2 + bucket));
        }
        assertTrue(buckets.size() >= 10);
        assertEquals(1.0D, DroppedReservoirExchangeHandler.ELAPSED_SECONDS);
    }

    @Test
    void exactHookContextRejectsClientRemovedAliasedAndStackedItems() {
        assertTrue(DroppedReservoirExchangeHandler.isExactServerItemContext(
                false, true, true, 1));
        assertFalse(DroppedReservoirExchangeHandler.isExactServerItemContext(
                true, true, true, 1));
        assertFalse(DroppedReservoirExchangeHandler.isExactServerItemContext(
                false, false, true, 1));
        assertFalse(DroppedReservoirExchangeHandler.isExactServerItemContext(
                false, true, false, 1));
        assertFalse(DroppedReservoirExchangeHandler.isExactServerItemContext(
                false, true, true, 2));
    }

    @Test
    void missingStateInitializesOnceFromRadiantEffectiveEnvironment() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();

        DroppedReservoirExchangeHandler.Status status =
                new DroppedReservoirExchangeHandler().exchangeInto(
                        stack, reservoir, 0.0D, 100.0D, 1.0D);

        assertEquals(DroppedReservoirExchangeHandler.Status.INITIALIZED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(1, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertEquals(13.333333333333334D, state.coreTemperatureC(), 1.0e-5D);
        assertEquals(state.coreTemperatureC(), state.surfaceTemperatureC());
    }

    @Test
    void exposedStateUsesDroppedConductanceAndRadiationWithOneWrite() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(0.0D, 0.0D).writeTo(stack);

        DroppedReservoirExchangeHandler.Status status =
                new DroppedReservoirExchangeHandler().exchangeInto(
                        stack, reservoir, 0.0D, 100.0D, 1.0D);

        assertEquals(DroppedReservoirExchangeHandler.Status.APPLIED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(1, reservoir.setCount);
        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertTrue(state.surfaceTemperatureC() > 0.0D);
        assertTrue(state.surfaceTemperatureC() > state.coreTemperatureC());
    }

    @Test
    void equalOrInvalidSamplesDoNotRewriteState() {
        CountingReservoir reservoir = new CountingReservoir();
        ItemStack equal = stack();
        new WearableThermalState(5.0D, 5.0D).writeTo(equal);
        DroppedReservoirExchangeHandler handler =
                new DroppedReservoirExchangeHandler();

        assertEquals(DroppedReservoirExchangeHandler.Status.UNCHANGED,
                handler.exchangeInto(equal, reservoir, 5.0D, 0.0D, 1.0D));
        assertEquals(0, reservoir.setCount);
        assertEquals(DroppedReservoirExchangeHandler.Status.NUMERIC_DEGRADED,
                handler.exchangeInto(
                        equal, reservoir, 5.0D, 0.0D, Double.NaN));
        assertEquals(0, reservoir.setCount);
    }

    @Test
    void staleObservationAdvancesWithAirOnlyInsteadOfFutureRadiation() {
        ItemStack staleStack = stack();
        ItemStack freshStack = stack();
        new WearableThermalState(0.0D, 0.0D).writeTo(staleStack);
        new WearableThermalState(0.0D, 0.0D).writeTo(freshStack);
        CountingReservoir staleReservoir = new CountingReservoir();
        CountingReservoir freshReservoir = new CountingReservoir();
        DroppedReservoirExchangeHandler handler =
                new DroppedReservoirExchangeHandler();

        DroppedReservoirExchangeHandler.Status staleStatus =
                handler.exchangeObservedInto(
                        staleStack,
                        staleReservoir,
                        10.0D,
                        100.0D,
                        99L,
                        100L,
                        1.0D);
        DroppedReservoirExchangeHandler.Status freshStatus =
                handler.exchangeObservedInto(
                        freshStack,
                        freshReservoir,
                        10.0D,
                        100.0D,
                        100L,
                        100L,
                        1.0D);

        assertEquals(DroppedReservoirExchangeHandler.Status.APPLIED, staleStatus);
        assertEquals(DroppedReservoirExchangeHandler.Status.APPLIED, freshStatus);
        double staleSurface = WearableThermalState.read(staleStack)
                .orElseThrow().surfaceTemperatureC();
        double freshSurface = WearableThermalState.read(freshStack)
                .orElseThrow().surfaceTemperatureC();
        assertTrue(staleSurface > 0.0D);
        assertTrue(freshSurface > staleSurface);
        assertFalse(DroppedReservoirExchangeHandler.isRadiationSampleFresh(
                99L, 100L));
        assertTrue(DroppedReservoirExchangeHandler.isRadiationSampleFresh(
                100L, 100L));
    }

    @Test
    void lifecycleMetadataIsTransientAndEntityRemovalNeedsNoStateCleanup() {
        for (java.lang.reflect.Field field
                : DroppedReservoirExchangeHandler.class.getDeclaredFields()) {
            assertFalse(Map.class.isAssignableFrom(field.getType()));
        }

        UUID identity = new UUID(17L, 29L);
        ItemStack stack = stack();
        new WearableThermalState(42.0D, 17.0D).writeTo(stack);
        CompoundTag beforeReset = stack.getTag().copy();
        assertFalse(DroppedReservoirExchangeHandler.isCadenceTick(identity, 0));
        assertEquals(beforeReset, stack.getTag());

        CompoundTag root = stack.getTag();
        assertEquals(Set.of(WearableThermalState.ROOT_KEY), root.getAllKeys());
        assertEquals(Set.of(
                        WearableThermalState.VERSION_KEY,
                        WearableThermalState.INITIALIZED_KEY,
                        WearableThermalState.CORE_TEMPERATURE_KEY,
                        WearableThermalState.SURFACE_TEMPERATURE_KEY),
                root.getCompound(WearableThermalState.ROOT_KEY).getAllKeys());
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
