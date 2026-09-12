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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryThermalExchangeHandlerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sourceDefaultCadenceIsExactlyTwentyTicks() {
        assertTrue(InventoryThermalExchangeHandler.isCadenceTick(0));
        assertTrue(InventoryThermalExchangeHandler.isCadenceTick(20));
        assertTrue(InventoryThermalExchangeHandler.isCadenceTick(40));
        assertFalse(InventoryThermalExchangeHandler.isCadenceTick(1));
        assertFalse(InventoryThermalExchangeHandler.isCadenceTick(19));
        assertEquals(1.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);
    }

    @Test
    void hookContextRequiresServerSideServerPlayer() {
        assertTrue(InventoryThermalExchangeHandler
                .isServerPlayerInventoryContext(false, true));
        assertFalse(InventoryThermalExchangeHandler
                .isServerPlayerInventoryContext(true, true));
        assertFalse(InventoryThermalExchangeHandler
                .isServerPlayerInventoryContext(false, false));
        assertFalse(InventoryThermalExchangeHandler
                .isServerPlayerInventoryContext(true, false));
    }

    @Test
    void ordinaryInventoryRequiresTheExactSlotObject() {
        ItemStack stack = stack();
        ItemStack equalCopy = stack.copy();
        List<ItemStack> ordinaryItems = List.of(stack, equalCopy);

        assertTrue(InventoryThermalExchangeHandler.isOrdinaryInventoryStack(
                ordinaryItems, 0, stack));
        assertFalse(InventoryThermalExchangeHandler.isOrdinaryInventoryStack(
                ordinaryItems, 1, stack));
        assertFalse(InventoryThermalExchangeHandler.isOrdinaryInventoryStack(
                ordinaryItems, -1, stack));
        assertFalse(InventoryThermalExchangeHandler.isOrdinaryInventoryStack(
                ordinaryItems, ordinaryItems.size(), stack));
    }

    @Test
    void missingStateInitializesOnceWithoutAdvancing() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        InventoryThermalExchangeHandler handler =
                new InventoryThermalExchangeHandler();

        InventoryThermalExchangeHandler.Status status = handler.exchangeInto(
                20, stack, reservoir, ItemStack.EMPTY,
                -12.5D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.INITIALIZED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(1, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertEquals(-12.5D, state.coreTemperatureC());
        assertEquals(-12.5D, state.surfaceTemperatureC());
    }

    @Test
    void initializedStateAdvancesTowardAirWithOneWrite() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(60.0D, 60.0D).writeTo(stack);

        InventoryThermalExchangeHandler.Status status =
                new InventoryThermalExchangeHandler().exchangeInto(
                        20, stack, reservoir, ItemStack.EMPTY,
                        0.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.APPLIED, status);
        assertEquals(1, status.stackWriteCount());
        assertEquals(0, reservoir.restoreCount);
        assertEquals(1, reservoir.setCount);
        WearableThermalState state = WearableThermalState.read(stack).orElseThrow();
        assertTrue(state.surfaceTemperatureC() < 60.0D);
        assertTrue(state.surfaceTemperatureC() < state.coreTemperatureC());
    }

    @Test
    void equippedStackIsExcludedBeforeStateOrSolverWork() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();

        InventoryThermalExchangeHandler.Status status =
                new InventoryThermalExchangeHandler().exchangeInto(
                        20, stack, reservoir, stack,
                        0.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.EQUIPPED, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(0, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        assertFalse(stack.hasTag());
    }

    @Test
    void sameStackCannotAdvanceTwiceInOnePlayerTick() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(60.0D, 60.0D).writeTo(stack);
        InventoryThermalExchangeHandler handler =
                new InventoryThermalExchangeHandler();

        InventoryThermalExchangeHandler.Status first = handler.exchangeInto(
                20, stack, reservoir, ItemStack.EMPTY,
                0.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);
        CompoundTag afterFirst = stack.getTag().copy();
        InventoryThermalExchangeHandler.Status duplicate = handler.exchangeInto(
                20, stack, reservoir, ItemStack.EMPTY,
                0.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.APPLIED, first);
        assertEquals(InventoryThermalExchangeHandler.Status.DUPLICATE, duplicate);
        assertEquals(0, duplicate.stackWriteCount());
        assertEquals(1, reservoir.setCount);
        assertEquals(afterFirst, stack.getTag());

        InventoryThermalExchangeHandler.Status nextTick = handler.exchangeInto(
                40, stack, reservoir, ItemStack.EMPTY,
                0.0D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);
        assertEquals(InventoryThermalExchangeHandler.Status.APPLIED, nextTick);
        assertEquals(2, reservoir.setCount);
        assertNotEquals(afterFirst, stack.getTag());
    }

    @Test
    void invalidEnvironmentCannotInitializeOrWrite() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();

        InventoryThermalExchangeHandler.Status status =
                new InventoryThermalExchangeHandler().exchangeInto(
                        20, stack, reservoir, ItemStack.EMPTY,
                        Double.NaN, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.STATE_UNAVAILABLE, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(1, reservoir.restoreCount);
        assertEquals(0, reservoir.setCount);
        assertFalse(stack.hasTag());
    }

    @Test
    void invalidElapsedIsAtomicAndDoesNotWrite() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(60.0D, 20.0D).writeTo(stack);
        CompoundTag before = stack.getTag().copy();

        InventoryThermalExchangeHandler.Status status =
                new InventoryThermalExchangeHandler().exchangeInto(
                        20, stack, reservoir, ItemStack.EMPTY,
                        0.0D, Double.POSITIVE_INFINITY);

        assertEquals(InventoryThermalExchangeHandler.Status.NUMERIC_DEGRADED, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(0, reservoir.setCount);
        assertEquals(before, stack.getTag());
    }

    @Test
    void nonReservoirAndEqualTemperaturesDoNotWrite() {
        ItemStack nonReservoir = stack();
        InventoryThermalExchangeHandler handler =
                new InventoryThermalExchangeHandler();
        assertEquals(InventoryThermalExchangeHandler.Status.NOT_WEARABLE,
                handler.exchangeInto(20, nonReservoir, null,
                        ItemStack.EMPTY, 0.0D, 1.0D));

        ItemStack equal = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(5.0D, 5.0D).writeTo(equal);
        CompoundTag before = equal.getTag().copy();
        InventoryThermalExchangeHandler.Status equalStatus = handler.exchangeInto(
                21, equal, reservoir, ItemStack.EMPTY, 5.0D, 1.0D);

        assertEquals(InventoryThermalExchangeHandler.Status.UNCHANGED, equalStatus);
        assertEquals(0, reservoir.setCount);
        assertEquals(before, equal.getTag());
    }

    @Test
    void subFloatTemperatureChangeDoesNotRewriteIdenticalNbt() {
        ItemStack stack = stack();
        CountingReservoir reservoir = new CountingReservoir();
        new WearableThermalState(5.0D, 5.0D).writeTo(stack);
        CompoundTag before = stack.getTag().copy();

        InventoryThermalExchangeHandler.Status status =
                new InventoryThermalExchangeHandler().exchangeInto(
                        20, stack, reservoir, ItemStack.EMPTY,
                        5.000001D, InventoryThermalExchangeHandler.ELAPSED_SECONDS);

        assertEquals(InventoryThermalExchangeHandler.Status.UNCHANGED, status);
        assertEquals(0, status.stackWriteCount());
        assertEquals(0, reservoir.setCount);
        assertEquals(before, stack.getTag());
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
