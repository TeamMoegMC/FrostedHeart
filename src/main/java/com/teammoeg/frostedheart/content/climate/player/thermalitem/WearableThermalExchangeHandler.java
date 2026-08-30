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
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/** Applies the fixed wearable exchange contract to one player and one ItemStack. */
public final class WearableThermalExchangeHandler {
    public static final double NORMAL_PLAYER_TEMPERATURE_C = 37.0D;

    private final ThreeNodeWearableHeatExchange.MutableResult exchangeResult =
            new ThreeNodeWearableHeatExchange.MutableResult();
    private final ThreeNodeWearableHeatExchange.Scratch exchangeScratch =
            new ThreeNodeWearableHeatExchange.Scratch();

    public enum Status {
        NOT_WEARABLE(0),
        STATE_UNAVAILABLE(0),
        INITIALIZED(1),
        UNCHANGED(0),
        NUMERIC_DEGRADED(0),
        PLAYER_UPDATE_REJECTED(0),
        APPLIED(1);

        private final int stackWriteCount;

        Status(int stackWriteCount) {
            this.stackWriteCount = stackWriteCount;
        }

        public int stackWriteCount() {
            return stackWriteCount;
        }
    }

    /** Management modes skip before slot lookup, initialization, or exchange. */
    public static boolean allowsPlayerExchange(
            boolean creative,
            boolean spectator,
            boolean invulnerable
    ) {
        return !creative && !spectator && !invulnerable;
    }

    public Status exchangeInto(
            PlayerTemperatureData playerData,
            ItemStack stack,
            double serverEnvironmentTemperatureC,
            double elapsedSeconds
    ) {
        Objects.requireNonNull(playerData, "playerData");
        if (stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof WearableThermalReservoir reservoir)) {
            return Status.NOT_WEARABLE;
        }
        return exchangeInto(
                playerData,
                stack,
                reservoir,
                serverEnvironmentTemperatureC,
                elapsedSeconds
        );
    }

    Status exchangeInto(
            PlayerTemperatureData playerData,
            ItemStack stack,
            WearableThermalReservoir reservoir,
            double serverEnvironmentTemperatureC,
            double elapsedSeconds
    ) {
        Objects.requireNonNull(playerData, "playerData");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(reservoir, "reservoir");

        Optional<WearableThermalState> currentState = reservoir.thermalState(stack);
        if (currentState.isEmpty()) {
            return reservoir.restoreOrInitializeThermalStateForServer(
                    stack, serverEnvironmentTemperatureC).isPresent()
                    ? Status.INITIALIZED
                    : Status.STATE_UNAVAILABLE;
        }

        WearableThermalState state = currentState.get();
        double playerTemperatureC = playerData.getCoreBodyTemp()
                + NORMAL_PLAYER_TEMPERATURE_C;
        ThermalExchangeKernel.Status exchangeStatus =
                ThreeNodeWearableHeatExchange.exchangeInto(
                        reservoir.thermalProfile(stack),
                        state.coreTemperatureC(),
                        state.surfaceTemperatureC(),
                        playerTemperatureC,
                        elapsedSeconds,
                        exchangeResult,
                        exchangeScratch
                );
        if (exchangeStatus != ThermalExchangeKernel.Status.APPLIED) {
            return Status.NUMERIC_DEGRADED;
        }

        double nextCoreTemperatureC = exchangeResult.reservoirCoreTemperatureC();
        double nextSurfaceTemperatureC = exchangeResult.reservoirSurfaceTemperatureC();
        if (!validReservoirTemperature(nextCoreTemperatureC)
                || !validReservoirTemperature(nextSurfaceTemperatureC)) {
            return Status.NUMERIC_DEGRADED;
        }

        float playerDelta = (float) (exchangeResult.playerTemperatureC()
                - playerTemperatureC);
        if (!Float.isFinite(playerDelta)) {
            return Status.PLAYER_UPDATE_REJECTED;
        }
        if (playerDelta == 0.0F
                && nextCoreTemperatureC == state.coreTemperatureC()
                && nextSurfaceTemperatureC == state.surfaceTemperatureC()) {
            return Status.UNCHANGED;
        }
        if (!playerData.applyCoreBodyTemperatureDelta(playerDelta)) {
            return Status.PLAYER_UPDATE_REJECTED;
        }

        reservoir.setTemperaturesC(
                stack, nextCoreTemperatureC, nextSurfaceTemperatureC);
        return Status.APPLIED;
    }

    private static boolean validReservoirTemperature(double temperatureC) {
        return Double.isFinite(temperatureC)
                && temperatureC >= WearableThermalState.MINIMUM_TEMPERATURE_C
                && temperatureC <= WearableThermalState.MAXIMUM_TEMPERATURE_C;
    }
}
