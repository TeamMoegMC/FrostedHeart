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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.Optional;

/**
 * 暖石类物品 version 1 的唯一持久化热状态。
 * <p>
 * Version-one ItemStack state for wearable thermal reservoirs. Temperatures are
 * absolute {@code degC}; a missing compound intentionally represents an
 * uninitialized reservoir and does not imply {@code 0 degC}.
 */
public record WearableThermalState(
        double coreTemperatureC,
        double surfaceTemperatureC
) {
    public static final String ROOT_KEY = "frostedheart:thermal_reservoir";
    public static final int SCHEMA_VERSION = 1;
    public static final String VERSION_KEY = "version";
    public static final String INITIALIZED_KEY = "initialized";
    public static final String CORE_TEMPERATURE_KEY = "core_temperature_c";
    public static final String SURFACE_TEMPERATURE_KEY = "surface_temperature_c";
    public static final double MINIMUM_TEMPERATURE_C = -1_000.0D;
    public static final double MAXIMUM_TEMPERATURE_C = 1_000.0D;

    private static final Logger LOGGER = LogManager.getLogger(WearableThermalState.class);
    private static final EnumSet<ReadFailure> REPORTED_FAILURES =
            EnumSet.noneOf(ReadFailure.class);

    public WearableThermalState {
        if (!isValidTemperature(coreTemperatureC)
                || !isValidTemperature(surfaceTemperatureC)) {
            throw new IllegalArgumentException(
                    "thermal reservoir temperatures must be finite and within [-1000, 1000] degC");
        }
    }

    /**
     * 读取已验证的 version 1 状态；读取过程不创建或修改 NBT。
     * <p>
     * Reads only a valid version-one state and never creates or changes ItemStack NBT.
     */
    public static Optional<WearableThermalState> read(ItemStack stack) {
        return inspect(stack).state();
    }

    /**
     * 在服务端环境样本可用时恢复或初始化状态。
     * <p>
     * Restores a valid state or initializes/repairs it to the finite server
     * environment temperature. A missing or invalid environment leaves the stack
     * untouched and uninitialized for this tick.
     */
    public static Optional<WearableThermalState> restoreOrInitializeForServer(
            ItemStack stack,
            double serverEnvironmentTemperatureC
    ) {
        ReadResult read = inspect(stack);
        if (read.state().isPresent()) {
            return read.state();
        }
        if (!Double.isFinite(serverEnvironmentTemperatureC)) {
            reportOnce(read.failure());
            return Optional.empty();
        }

        WearableThermalState repaired = new WearableThermalState(
                clampTemperature(serverEnvironmentTemperatureC),
                clampTemperature(serverEnvironmentTemperatureC)
        );
        repaired.writeTo(stack);
        reportOnce(read.failure());
        return Optional.of(repaired);
    }

    /** Writes this validated state as the complete version-one reservoir compound. */
    public void writeTo(ItemStack stack) {
        CompoundTag reservoir = new CompoundTag();
        reservoir.putInt(VERSION_KEY, SCHEMA_VERSION);
        reservoir.putBoolean(INITIALIZED_KEY, true);
        reservoir.putFloat(CORE_TEMPERATURE_KEY, (float) coreTemperatureC);
        reservoir.putFloat(SURFACE_TEMPERATURE_KEY, (float) surfaceTemperatureC);
        stack.getOrCreateTag().put(ROOT_KEY, reservoir);
    }

    public static double clampTemperature(double temperatureC) {
        return Math.max(MINIMUM_TEMPERATURE_C,
                Math.min(MAXIMUM_TEMPERATURE_C, temperatureC));
    }

    private static ReadResult inspect(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return new ReadResult(Optional.empty(), ReadFailure.MISSING);
        }
        CompoundTag root = stack.getTag();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return new ReadResult(Optional.empty(), ReadFailure.MISSING);
        }
        CompoundTag reservoir = root.getCompound(ROOT_KEY);
        if (!reservoir.contains(VERSION_KEY, Tag.TAG_ANY_NUMERIC)
                || reservoir.getInt(VERSION_KEY) != SCHEMA_VERSION) {
            return new ReadResult(Optional.empty(), ReadFailure.UNKNOWN_SCHEMA);
        }
        if (!reservoir.contains(INITIALIZED_KEY, Tag.TAG_BYTE)) {
            return new ReadResult(Optional.empty(), ReadFailure.MALFORMED_STATE);
        }
        if (!reservoir.getBoolean(INITIALIZED_KEY)) {
            return new ReadResult(Optional.empty(), ReadFailure.UNINITIALIZED);
        }
        if (!reservoir.contains(CORE_TEMPERATURE_KEY, Tag.TAG_ANY_NUMERIC)
                || !reservoir.contains(SURFACE_TEMPERATURE_KEY, Tag.TAG_ANY_NUMERIC)) {
            return new ReadResult(Optional.empty(), ReadFailure.MALFORMED_STATE);
        }

        double coreTemperatureC = reservoir.getDouble(CORE_TEMPERATURE_KEY);
        double surfaceTemperatureC = reservoir.getDouble(SURFACE_TEMPERATURE_KEY);
        if (!isValidTemperature(coreTemperatureC)
                || !isValidTemperature(surfaceTemperatureC)) {
            return new ReadResult(Optional.empty(), ReadFailure.INVALID_TEMPERATURE);
        }
        return new ReadResult(
                Optional.of(new WearableThermalState(coreTemperatureC, surfaceTemperatureC)),
                null
        );
    }

    private static boolean isValidTemperature(double temperatureC) {
        return Double.isFinite(temperatureC)
                && temperatureC >= MINIMUM_TEMPERATURE_C
                && temperatureC <= MAXIMUM_TEMPERATURE_C;
    }

    private static void reportOnce(ReadFailure failure) {
        if (failure == null || failure == ReadFailure.MISSING
                || failure == ReadFailure.UNINITIALIZED) {
            return;
        }
        synchronized (REPORTED_FAILURES) {
            if (!REPORTED_FAILURES.add(failure)) {
                return;
            }
        }
        LOGGER.warn("Wearable thermal reservoir state is invalid: {}", failure.logLabel);
    }

    private record ReadResult(Optional<WearableThermalState> state, ReadFailure failure) {
    }

    private enum ReadFailure {
        MISSING("missing state"),
        UNINITIALIZED("uninitialized state"),
        UNKNOWN_SCHEMA("unknown schema"),
        MALFORMED_STATE("malformed state"),
        INVALID_TEMPERATURE("invalid temperature");

        private final String logLabel;

        ReadFailure(String logLabel) {
            this.logLabel = logLabel;
        }
    }
}
