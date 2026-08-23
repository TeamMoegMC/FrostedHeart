/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

/** SI unit conversions and the enthalpy reference contract for thermal prototypes. */
public final class ThermalUnits {
    public static final double TICKS_PER_SECOND = 20.0;

    private ThermalUnits() {
    }

    public static double ticksToSeconds(long ticks) {
        if (ticks < 0L) {
            throw new IllegalArgumentException("ticks must be non-negative");
        }
        return ticks / TICKS_PER_SECOND;
    }

    /**
     * Converts an absolute temperature to sensible enthalpy using
     * {@code H = C * (T - Tref)}.
     */
    public static double enthalpyFromTemperature(
            double temperatureC,
            double referenceTemperatureC,
            double capacityJPerK
    ) {
        requireFinite("temperatureC", temperatureC);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requirePositive("capacityJPerK", capacityJPerK);
        return requireFiniteResult(
                "enthalpy",
                capacityJPerK * (temperatureC - referenceTemperatureC)
        );
    }

    /** Converts sensible enthalpy back to absolute temperature. */
    public static double temperatureFromEnthalpy(
            double enthalpyJ,
            double referenceTemperatureC,
            double capacityJPerK
    ) {
        requireFinite("enthalpyJ", enthalpyJ);
        requireFinite("referenceTemperatureC", referenceTemperatureC);
        requirePositive("capacityJPerK", capacityJPerK);
        return requireFiniteResult(
                "temperature",
                referenceTemperatureC + enthalpyJ / capacityJPerK
        );
    }

    static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    static void requireNonNegative(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    static void requirePositive(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    static double requireFiniteResult(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException(name + " exceeded the finite reference domain");
        }
        return value;
    }
}
