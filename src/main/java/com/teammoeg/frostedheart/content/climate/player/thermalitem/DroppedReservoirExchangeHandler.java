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

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiantEquivalentTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Server-only exchange for the single ItemEntity supplied by the item hook. */
public final class DroppedReservoirExchangeHandler {
    public static final int CADENCE_TICKS = 20;
    public static final double ELAPSED_SECONDS = CADENCE_TICKS / 20.0D;

    private final ReservoirEnvironmentExchange.MutableResult result =
            new ReservoirEnvironmentExchange.MutableResult();
    private final ReservoirEnvironmentExchange.Scratch scratch =
            new ReservoirEnvironmentExchange.Scratch();
    private final ThermalEnvironmentSample environment =
            new ThermalEnvironmentSample();

    public enum Status {
        NOT_CADENCE(0),
        INVALID_CONTEXT(0),
        NOT_WEARABLE(0),
        STATE_UNAVAILABLE(0),
        INITIALIZED(1),
        UNCHANGED(0),
        NUMERIC_DEGRADED(0),
        APPLIED(1);

        private final int stackWriteCount;

        Status(int stackWriteCount) {
            this.stackWriteCount = stackWriteCount;
        }

        public int stackWriteCount() {
            return stackWriteCount;
        }
    }

    /** Called only from {@link WarmStoneItem#onEntityItemUpdate}. */
    public Status tickItemEntity(ItemStack stack, ItemEntity entity) {
        if (!isExactServerItemContext(
                entity.level().isClientSide,
                entity.isAlive(),
                entity.getItem() == stack,
                stack.getCount())) {
            return Status.INVALID_CONTEXT;
        }
        if (!isCadenceTick(entity.getUUID(), entity.tickCount)) {
            return Status.NOT_CADENCE;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return Status.INVALID_CONTEXT;
        }

        double naturalTemperatureC = WorldTemperature.naturalAir(
                serverLevel, entity.blockPosition());
        MinecraftThermalInput.gameplayItemEnvironment(
                entity,
                naturalTemperatureC,
                environment);
        double airTemperatureC = environment.airAvailable()
                ? environment.airTemperatureC() : naturalTemperatureC;
        return exchangeInto(
                stack,
                stack.getItem() instanceof WearableThermalReservoir reservoir
                        ? reservoir : null,
                airTemperatureC,
                environment.radiantFluxWPerM2(),
                ELAPSED_SECONDS);
    }

    static boolean isExactServerItemContext(
            boolean clientSide,
            boolean alive,
            boolean exactStack,
            int stackCount
    ) {
        return !clientSide && alive && exactStack && stackCount == 1;
    }

    static boolean isCadenceTick(UUID entityIdentity, int loadedTickCount) {
        if (entityIdentity == null || loadedTickCount < CADENCE_TICKS) {
            return false;
        }
        return Math.floorMod(loadedTickCount - CADENCE_TICKS, CADENCE_TICKS)
                == cadenceBucket(entityIdentity);
    }

    static int cadenceBucket(UUID entityIdentity) {
        long mixed = entityIdentity.getMostSignificantBits()
                ^ Long.rotateLeft(entityIdentity.getLeastSignificantBits(), 17);
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return Math.floorMod((int) (mixed ^ mixed >>> 32), CADENCE_TICKS);
    }

    Status exchangeInto(
            ItemStack stack,
            WearableThermalReservoir reservoir,
            double airTemperatureC,
            double radiantFluxWPerM2,
            double elapsedSeconds
    ) {
        if (reservoir == null || stack == null || stack.isEmpty()) {
            return Status.NOT_WEARABLE;
        }

        double effectiveEnvironmentTemperatureC =
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(
                        airTemperatureC, radiantFluxWPerM2);
        Optional<WearableThermalState> existingState = reservoir.thermalState(stack);
        if (existingState.isEmpty()) {
            Optional<WearableThermalState> restored =
                    reservoir.restoreOrInitializeThermalStateForServer(
                            stack, effectiveEnvironmentTemperatureC);
            return restored.isPresent()
                    ? Status.INITIALIZED : Status.STATE_UNAVAILABLE;
        }

        WearableThermalState state = existingState.orElseThrow();
        ReservoirEnvironmentExchange.Status exchangeStatus =
                ReservoirEnvironmentExchange.advanceDroppedInto(
                        reservoir.thermalProfile(stack),
                        state.coreTemperatureC(),
                        state.surfaceTemperatureC(),
                        airTemperatureC,
                        radiantFluxWPerM2,
                        elapsedSeconds,
                        scratch,
                        result);
        if (exchangeStatus != ReservoirEnvironmentExchange.Status.APPLIED
                || !validStateTemperature(result.coreTemperatureC())
                || !validStateTemperature(result.surfaceTemperatureC())) {
            return Status.NUMERIC_DEGRADED;
        }
        if (Float.compare((float) state.coreTemperatureC(),
                (float) result.coreTemperatureC()) == 0
                && Float.compare((float) state.surfaceTemperatureC(),
                (float) result.surfaceTemperatureC()) == 0) {
            return Status.UNCHANGED;
        }

        reservoir.setTemperaturesC(
                stack, result.coreTemperatureC(), result.surfaceTemperatureC());
        return Status.APPLIED;
    }

    private static boolean validStateTemperature(double temperatureC) {
        return Double.isFinite(temperatureC)
                && temperatureC >= WearableThermalState.MINIMUM_TEMPERATURE_C
                && temperatureC <= WearableThermalState.MAXIMUM_TEMPERATURE_C;
    }
}
