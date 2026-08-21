/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.transport;

/** Forge-independent formulas and comparison rules for transport-capacity reservations. */
public final class TransportReservationModel {
    private static final int COMPARISON_ULPS = 8;

    private TransportReservationModel() {
    }

    public static double warehouseScaleMetric(double warehouseVolume) {
        if (!Double.isFinite(warehouseVolume) || warehouseVolume < 0.0) {
            return Double.NaN;
        }
        return Math.sqrt(warehouseVolume);
    }

    public static double warehouseScaleFactor(double scaleMetric, TransportConsumerParameters parameters) {
        if (!isFiniteNonNegative(scaleMetric) || parameters == null) {
            return Double.NaN;
        }
        double factor = 1.0 + parameters.warehouseScaleCostPerMetric() * scaleMetric;
        return isFiniteNonNegative(factor) ? factor : Double.NaN;
    }

    public static double requiredCapacity(
            TransportEndpointKind endpointKind,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportConsumerParameters parameters
    ) {
        if (endpointKind == null || parameters == null
                || !parameters.isRateValid(rateItemsPerSecond)) {
            return Double.NaN;
        }
        if (rateItemsPerSecond == 0) {
            return 0.0;
        }
        return capacityForStoredRate(endpointKind, rateItemsPerSecond, scaleMetric, parameters);
    }

    /** Recomputes a persisted rate even if a later config lowers the player-selectable maximum. */
    public static double capacityForStoredRate(
            TransportEndpointKind endpointKind,
            int rateItemsPerSecond,
            double scaleMetric,
            TransportConsumerParameters parameters
    ) {
        if (endpointKind == null || parameters == null || rateItemsPerSecond < 0) {
            return Double.NaN;
        }
        if (rateItemsPerSecond == 0) {
            return 0.0;
        }
        double factor = switch (endpointKind) {
            case WAREHOUSE_INTERFACE -> warehouseScaleFactor(scaleMetric, parameters);
        };
        if (!isFiniteNonNegative(factor)) {
            return Double.NaN;
        }
        double capacity = rateItemsPerSecond * factor;
        return isFiniteNonNegative(capacity) ? capacity : Double.NaN;
    }

    public static double comparisonMagnitude(double first, double second) {
        if (!isFiniteNonNegative(first) || !isFiniteNonNegative(second)) {
            return Double.NaN;
        }
        return Math.max(1.0, Math.max(Math.abs(first), Math.abs(second)));
    }

    public static double comparisonTolerance(double first, double second) {
        double magnitude = comparisonMagnitude(first, second);
        return Double.isNaN(magnitude) ? Double.NaN : COMPARISON_ULPS * Math.ulp(magnitude);
    }

    public static boolean lessThanOrNearlyEqual(double first, double second) {
        if (!isFiniteNonNegative(first) || !isFiniteNonNegative(second)) {
            return false;
        }
        return first <= second || first - second <= comparisonTolerance(first, second);
    }

    public static boolean meaningfullyGreater(double first, double second) {
        if (!isFiniteNonNegative(first) || !isFiniteNonNegative(second)) {
            return false;
        }
        return first - second > comparisonTolerance(first, second);
    }

    public static double effectiveRateScale(double totalCapacity, double reservedCapacity) {
        if (!isFiniteNonNegative(totalCapacity) || !isFiniteNonNegative(reservedCapacity)) {
            return Double.NaN;
        }
        if (reservedCapacity == 0.0 || !meaningfullyGreater(reservedCapacity, totalCapacity)) {
            return 1.0;
        }
        return totalCapacity / reservedCapacity;
    }

    public static AdmissionEvaluation evaluateAdmission(
            double totalCapacity,
            double currentReservedCapacity,
            double oldEndpointReservedCapacity,
            double candidateEndpointReservedCapacity
    ) {
        if (!isFiniteNonNegative(totalCapacity)
                || !isFiniteNonNegative(currentReservedCapacity)
                || !isFiniteNonNegative(oldEndpointReservedCapacity)
                || !isFiniteNonNegative(candidateEndpointReservedCapacity)) {
            return AdmissionEvaluation.invalid();
        }
        double candidateTownReserved = currentReservedCapacity - oldEndpointReservedCapacity
                + candidateEndpointReservedCapacity;
        if (!isFiniteNonNegative(candidateTownReserved)) {
            return AdmissionEvaluation.invalid();
        }
        boolean doesNotIncreaseEndpoint = lessThanOrNearlyEqual(
                candidateEndpointReservedCapacity, oldEndpointReservedCapacity);
        boolean accepted = doesNotIncreaseEndpoint
                || lessThanOrNearlyEqual(candidateTownReserved, totalCapacity);
        return new AdmissionEvaluation(true, accepted, doesNotIncreaseEndpoint,
                candidateTownReserved,
                Math.max(0.0, candidateEndpointReservedCapacity - oldEndpointReservedCapacity));
    }

    public static boolean isFiniteNonNegative(double value) {
        return Double.isFinite(value) && value >= 0.0;
    }

    public record AdmissionEvaluation(
            boolean valid,
            boolean accepted,
            boolean doesNotIncreaseEndpoint,
            double candidateTownReservedCapacity,
            double requiredAdditionalCapacity
    ) {
        private static AdmissionEvaluation invalid() {
            return new AdmissionEvaluation(false, false, false, Double.NaN, Double.NaN);
        }
    }
}
