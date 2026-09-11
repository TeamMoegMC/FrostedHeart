/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Objects;

/** Symmetric gameplay-scale buoyancy adjustment for vertical air conductance. */
public final class BuoyancyConductance {
    private BuoyancyConductance() {
    }

    public record Parameters(
            double minimumFactor,
            double maximumFactor,
            double temperatureScaleK
    ) {
        public Parameters {
            if (!Double.isFinite(minimumFactor) || minimumFactor < 0.0D) {
                throw new IllegalArgumentException("minimumFactor must be finite and non-negative");
            }
            if (!Double.isFinite(maximumFactor) || maximumFactor < minimumFactor) {
                throw new IllegalArgumentException(
                        "maximumFactor must be finite and at least minimumFactor");
            }
            if (!Double.isFinite(temperatureScaleK) || temperatureScaleK <= 0.0D) {
                throw new IllegalArgumentException("temperatureScaleK must be finite and positive");
            }
        }
    }

    public enum Status {
        APPLIED,
        NUMERIC_DEGRADED
    }

    /** Caller-owned result for allocation-free pair traversal. */
    public static final class MutableResult {
        private Status status = Status.NUMERIC_DEGRADED;
        private double conductanceWPerK = Double.NaN;

        public boolean applied() {
            return status == Status.APPLIED;
        }

        public double conductanceWPerK() {
            return conductanceWPerK;
        }

        private void set(Status nextStatus, double nextConductance) {
            status = nextStatus;
            conductanceWPerK = nextConductance;
        }

    }

    /**
     * Evaluates from physical lower/upper positions, so swapping A and B does
     * not change the result. Equal-height pairs receive the clamped neutral factor.
     */
    public static Status evaluateInto(
            double baseConductanceWPerK,
            double temperatureAC,
            double centerYA,
            double temperatureBC,
            double centerYB,
            Parameters parameters,
            MutableResult result
    ) {
        Objects.requireNonNull(result, "result");
        if (parameters == null
                || !nonNegativeFinite(baseConductanceWPerK)
                || !Double.isFinite(temperatureAC)
                || !Double.isFinite(centerYA)
                || !Double.isFinite(temperatureBC)
                || !Double.isFinite(centerYB)) {
            result.set(Status.NUMERIC_DEGRADED, Double.NaN);
            return Status.NUMERIC_DEGRADED;
        }

        double rawFactor;
        if (centerYA == centerYB) {
            rawFactor = 1.0D;
        } else {
            double lowerTemperature = centerYA < centerYB ? temperatureAC : temperatureBC;
            double upperTemperature = centerYA < centerYB ? temperatureBC : temperatureAC;
            rawFactor = 1.0D
                    + (lowerTemperature - upperTemperature) / parameters.temperatureScaleK();
        }
        double factor = Math.max(
                parameters.minimumFactor(),
                Math.min(parameters.maximumFactor(), rawFactor)
        );
        double conductance = baseConductanceWPerK * factor;
        if (!Double.isFinite(factor) || !nonNegativeFinite(conductance)) {
            result.set(Status.NUMERIC_DEGRADED, Double.NaN);
            return Status.NUMERIC_DEGRADED;
        }
        result.set(Status.APPLIED, conductance);
        return Status.APPLIED;
    }

    private static boolean nonNegativeFinite(double value) {
        return value >= 0.0D && Double.isFinite(value);
    }
}
