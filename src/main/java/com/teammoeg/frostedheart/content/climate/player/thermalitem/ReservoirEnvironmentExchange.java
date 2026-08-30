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

import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiantEquivalentTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalExchangeKernel;

import java.util.Objects;

/**
 * Fixed core/surface/environment exchange for a non-worn thermal reservoir.
 *
 * <p>The environment is an infinite fixed-temperature boundary connected only to the
 * surface. Each bounded substep applies a Strang sweep: half surface/environment,
 * full core/surface, then half surface/environment. The exchange primitives remain
 * owned by {@link ThermalExchangeKernel}; this class only supplies the fixed topology
 * and normalized-capacity unit conversion.</p>
 */
public final class ReservoirEnvironmentExchange {
    /** Version-one persistent thermal-state bounds, in absolute {@code degC}. */
    public static final double MINIMUM_TEMPERATURE_C = -1_000.0D;
    public static final double MAXIMUM_TEMPERATURE_C = 1_000.0D;
    /** Limits splitting error for the two non-commuting fixed topology edges. */
    public static final double MAXIMUM_SUBSTEP_SECONDS = 1.0D;
    /** Prevents a corrupted elapsed-time input from turning one item tick into unbounded work. */
    public static final int MAXIMUM_SUBSTEPS = 4_096;

    private ReservoirEnvironmentExchange() {
    }

    public enum Status {
        APPLIED,
        NUMERIC_DEGRADED
    }

    /** Caller-owned kernel state; reuse one instance for every stable tick of a reservoir. */
    public static final class Scratch {
        private final ThermalExchangeKernel.MutablePairResult coreSurface =
                new ThermalExchangeKernel.MutablePairResult();
        private final ThermalExchangeKernel.MutableBoundaryResult surfaceEnvironment =
                new ThermalExchangeKernel.MutableBoundaryResult();
    }

    /** Caller-owned result, kept mutable to avoid per-tick result allocation. */
    public static final class MutableResult {
        private Status status = Status.NUMERIC_DEGRADED;
        private double coreTemperatureC;
        private double surfaceTemperatureC;
        private double effectiveEnvironmentTemperatureC;

        public Status status() {
            return status;
        }

        public boolean applied() {
            return status == Status.APPLIED;
        }

        public double coreTemperatureC() {
            return coreTemperatureC;
        }

        public double surfaceTemperatureC() {
            return surfaceTemperatureC;
        }

        public double effectiveEnvironmentTemperatureC() {
            return effectiveEnvironmentTemperatureC;
        }

        private void set(
                Status nextStatus,
                double nextCoreTemperatureC,
                double nextSurfaceTemperatureC,
                double nextEffectiveEnvironmentTemperatureC
        ) {
            status = nextStatus;
            coreTemperatureC = nextCoreTemperatureC;
            surfaceTemperatureC = nextSurfaceTemperatureC;
            effectiveEnvironmentTemperatureC = nextEffectiveEnvironmentTemperatureC;
        }
    }

    /** Advances a covered inventory reservoir. Inventory never receives direct radiation. */
    public static Status advanceInventoryInto(
            WearableThermalProfile profile,
            double coreTemperatureC,
            double surfaceTemperatureC,
            double airTemperatureC,
            double elapsedSeconds,
            Scratch scratch,
            MutableResult result
    ) {
        return advanceInto(
                profile,
                coreTemperatureC,
                surfaceTemperatureC,
                airTemperatureC,
                profile == null ? Double.NaN
                        : profile.inventoryEnvironmentTransferRatePerSecond(),
                elapsedSeconds,
                scratch,
                result
        );
    }

    /** Advances an exposed dropped reservoir using the shared radiant equivalent boundary. */
    public static Status advanceDroppedInto(
            WearableThermalProfile profile,
            double coreTemperatureC,
            double surfaceTemperatureC,
            double airTemperatureC,
            double radiantFluxWPerM2,
            double elapsedSeconds,
            Scratch scratch,
            MutableResult result
    ) {
        return advanceInto(
                profile,
                coreTemperatureC,
                surfaceTemperatureC,
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(
                        airTemperatureC, radiantFluxWPerM2),
                profile == null ? Double.NaN
                        : profile.droppedEnvironmentTransferRatePerSecond(),
                elapsedSeconds,
                scratch,
                result
        );
    }

    /**
     * Advances the fixed core/surface/environment topology with an explicit normalized
     * surface-to-environment conductance. This is useful for deterministic model fixtures;
     * production callers should select {@link #advanceInventoryInto} or
     * {@link #advanceDroppedInto} so conductance stays derived from the frozen profile.
     */
    public static Status advanceInto(
            WearableThermalProfile profile,
            double coreTemperatureC,
            double surfaceTemperatureC,
            double effectiveEnvironmentTemperatureC,
            double surfaceEnvironmentConductancePerSecond,
            double elapsedSeconds,
            Scratch scratch,
            MutableResult result
    ) {
        Objects.requireNonNull(scratch, "scratch");
        Objects.requireNonNull(result, "result");

        double safeEnvironment = boundedTemperature(effectiveEnvironmentTemperatureC, 0.0D);
        double safeCore = boundedTemperature(coreTemperatureC, safeEnvironment);
        double safeSurface = boundedTemperature(surfaceTemperatureC, safeEnvironment);
        if (profile == null
                || !nonNegativeFinite(surfaceEnvironmentConductancePerSecond)
                || !nonNegativeFinite(elapsedSeconds)) {
            return degraded(result, safeCore, safeSurface, safeEnvironment);
        }

        double coreCapacity = profile.coreCapacityRatio();
        double surfaceCapacity = profile.surfaceCapacityRatio();
        double coreSurfaceConductance = profile.coreSurfaceTransferRatePerSecond();
        if (!positiveFinite(coreCapacity)
                || !positiveFinite(surfaceCapacity)
                || !positiveFinite(coreSurfaceConductance)) {
            return degraded(result, safeCore, safeSurface, safeEnvironment);
        }

        int substepCount = substepCount(elapsedSeconds);
        if (substepCount < 0) {
            return degraded(result, safeCore, safeSurface, safeEnvironment);
        }

        double coreEnthalpy = coreCapacity * (safeCore - safeEnvironment);
        double surfaceEnthalpy = surfaceCapacity * (safeSurface - safeEnvironment);
        if (!Double.isFinite(coreEnthalpy) || !Double.isFinite(surfaceEnthalpy)) {
            return degraded(result, safeCore, safeSurface, safeEnvironment);
        }

        double substepSeconds = substepCount == 0 ? 0.0D : elapsedSeconds / substepCount;
        for (int step = 0; step < substepCount; step++) {
            double halfStepSeconds = substepSeconds * 0.5D;
            if (ThermalExchangeKernel.exchangeFixedBoundaryInto(
                    surfaceEnthalpy,
                    surfaceCapacity,
                    safeEnvironment,
                    safeEnvironment,
                    surfaceEnvironmentConductancePerSecond,
                    halfStepSeconds,
                    scratch.surfaceEnvironment) != ThermalExchangeKernel.Status.APPLIED) {
                return degraded(result, safeCore, safeSurface, safeEnvironment);
            }
            surfaceEnthalpy = scratch.surfaceEnvironment.enthalpyJ();

            if (ThermalExchangeKernel.exchangePairInto(
                    coreEnthalpy,
                    coreCapacity,
                    surfaceEnthalpy,
                    surfaceCapacity,
                    coreSurfaceConductance,
                    substepSeconds,
                    scratch.coreSurface) != ThermalExchangeKernel.Status.APPLIED) {
                return degraded(result, safeCore, safeSurface, safeEnvironment);
            }
            coreEnthalpy = scratch.coreSurface.enthalpyAJ();
            surfaceEnthalpy = scratch.coreSurface.enthalpyBJ();

            if (ThermalExchangeKernel.exchangeFixedBoundaryInto(
                    surfaceEnthalpy,
                    surfaceCapacity,
                    safeEnvironment,
                    safeEnvironment,
                    surfaceEnvironmentConductancePerSecond,
                    halfStepSeconds,
                    scratch.surfaceEnvironment) != ThermalExchangeKernel.Status.APPLIED) {
                return degraded(result, safeCore, safeSurface, safeEnvironment);
            }
            surfaceEnthalpy = scratch.surfaceEnvironment.enthalpyJ();
        }

        double nextCore = safeEnvironment + coreEnthalpy / coreCapacity;
        double nextSurface = safeEnvironment + surfaceEnthalpy / surfaceCapacity;
        if (!Double.isFinite(nextCore) || !Double.isFinite(nextSurface)) {
            return degraded(result, safeCore, safeSurface, safeEnvironment);
        }
        result.set(
                Status.APPLIED,
                boundedTemperature(nextCore, safeEnvironment),
                boundedTemperature(nextSurface, safeEnvironment),
                safeEnvironment
        );
        return Status.APPLIED;
    }

    private static int substepCount(double elapsedSeconds) {
        if (elapsedSeconds == 0.0D) {
            return 0;
        }
        double required = Math.ceil(elapsedSeconds / MAXIMUM_SUBSTEP_SECONDS);
        if (!Double.isFinite(required) || required > MAXIMUM_SUBSTEPS) {
            return -1;
        }
        return (int) required;
    }

    private static Status degraded(
            MutableResult result,
            double coreTemperatureC,
            double surfaceTemperatureC,
            double effectiveEnvironmentTemperatureC
    ) {
        result.set(
                Status.NUMERIC_DEGRADED,
                coreTemperatureC,
                surfaceTemperatureC,
                effectiveEnvironmentTemperatureC
        );
        return Status.NUMERIC_DEGRADED;
    }

    private static double boundedTemperature(double value, double fallback) {
        double finite = Double.isFinite(value) ? value : fallback;
        return Math.max(MINIMUM_TEMPERATURE_C, Math.min(MAXIMUM_TEMPERATURE_C, finite));
    }

    private static boolean positiveFinite(double value) {
        return value > 0.0D && Double.isFinite(value);
    }

    private static boolean nonNegativeFinite(double value) {
        return value >= 0.0D && Double.isFinite(value);
    }
}
