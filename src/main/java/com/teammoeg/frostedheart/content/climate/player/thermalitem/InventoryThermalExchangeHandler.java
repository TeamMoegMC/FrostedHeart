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

import com.teammoeg.chorda.CompatModule;
import com.teammoeg.frostedheart.compat.curios.CuriosCompat;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Server-side ordinary-inventory exchange for a single thermal-reservoir stack.
 *
 * <p>The item {@code inventoryTick} hook supplies only the stack being ticked, so
 * this path never scans players or inventories. A handler-owned identity set is
 * replaced before the first claim from a different server tick and prevents a
 * malformed shared stack reference from advancing twice in one game tick.</p>
 */
public final class InventoryThermalExchangeHandler {
    public static final int CADENCE_TICKS = 20;
    public static final double ELAPSED_SECONDS = CADENCE_TICKS / 20.0D;

    private final ReservoirEnvironmentExchange.MutableResult result =
            new ReservoirEnvironmentExchange.MutableResult();
    private final ReservoirEnvironmentExchange.Scratch scratch =
            new ReservoirEnvironmentExchange.Scratch();
    private final IdentityHashMap<ItemStack, Boolean> processedStacks =
            new IdentityHashMap<>();
    private int processedServerTick = Integer.MIN_VALUE;

    public enum Status {
        NOT_CADENCE(0),
        OUTSIDE_ORDINARY_INVENTORY(0),
        EQUIPPED(0),
        DUPLICATE(0),
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

    /** Called only from {@link WarmStoneItem#inventoryTick}. */
    public Status tickServerPlayerInventoryStack(
            ItemStack stack,
            ServerPlayer player,
            int slot
    ) {
        if (!isCadenceTick(player.tickCount)) {
            return Status.NOT_CADENCE;
        }
        if (!isOrdinaryInventoryStack(player.getInventory().items, slot, stack)) {
            return Status.OUTSIDE_ORDINARY_INVENTORY;
        }
        if (!claim(player.getServer().getTickCount(), stack)) {
            return Status.DUPLICATE;
        }

        ItemStack equippedStack = CompatModule.isCuriosLoaded()
                ? CuriosCompat.getWearableThermalReservoirInWarmStoneSlot(player)
                : ItemStack.EMPTY;
        if (stack == equippedStack) {
            return Status.EQUIPPED;
        }

        double environmentTemperatureC = MinecraftThermalInput
                .gameplayPassiveEnvironment(
                        player.level(),
                        player.blockPosition(),
                        WorldTemperature.naturalAir(
                                player.level(), player.blockPosition())
                );
        return exchangeClaimedInto(
                stack,
                stack.getItem() instanceof WearableThermalReservoir reservoir
                        ? reservoir : null,
                environmentTemperatureC,
                ELAPSED_SECONDS
        );
    }

    static boolean isCadenceTick(int playerTickCount) {
        return Math.floorMod(playerTickCount, CADENCE_TICKS) == 0;
    }

    static boolean isServerPlayerInventoryContext(
            boolean clientSide,
            boolean serverPlayer
    ) {
        return !clientSide && serverPlayer;
    }

    static boolean isOrdinaryInventoryStack(
            List<ItemStack> ordinaryItems,
            int slot,
            ItemStack stack
    ) {
        return slot >= 0
                && slot < ordinaryItems.size()
                && ordinaryItems.get(slot) == stack;
    }

    Status exchangeInto(
            int serverTick,
            ItemStack stack,
            WearableThermalReservoir reservoir,
            ItemStack equippedStack,
            double environmentTemperatureC,
            double elapsedSeconds
    ) {
        if (!claim(serverTick, stack)) {
            return Status.DUPLICATE;
        }
        if (stack == equippedStack) {
            return Status.EQUIPPED;
        }
        return exchangeClaimedInto(
                stack, reservoir, environmentTemperatureC, elapsedSeconds);
    }

    private Status exchangeClaimedInto(
            ItemStack stack,
            WearableThermalReservoir reservoir,
            double environmentTemperatureC,
            double elapsedSeconds
    ) {
        if (reservoir == null || stack == null || stack.isEmpty()) {
            return Status.NOT_WEARABLE;
        }

        Optional<WearableThermalState> existingState = reservoir.thermalState(stack);
        if (existingState.isEmpty()) {
            Optional<WearableThermalState> restored =
                    reservoir.restoreOrInitializeThermalStateForServer(
                            stack, environmentTemperatureC);
            return restored.isPresent()
                    ? Status.INITIALIZED : Status.STATE_UNAVAILABLE;
        }

        WearableThermalState state = existingState.orElseThrow();
        ReservoirEnvironmentExchange.Status exchangeStatus =
                ReservoirEnvironmentExchange.advanceInventoryInto(
                        reservoir.thermalProfile(stack),
                        state.coreTemperatureC(),
                        state.surfaceTemperatureC(),
                        environmentTemperatureC,
                        elapsedSeconds,
                        scratch,
                        result
                );
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

    private boolean claim(int serverTick, ItemStack stack) {
        if (serverTick != processedServerTick) {
            processedStacks.clear();
            processedServerTick = serverTick;
        }
        return processedStacks.put(stack, Boolean.TRUE) == null;
    }

    private static boolean validStateTemperature(double temperatureC) {
        return Double.isFinite(temperatureC)
                && temperatureC >= WearableThermalState.MINIMUM_TEMPERATURE_C
                && temperatureC <= WearableThermalState.MAXIMUM_TEMPERATURE_C;
    }
}
