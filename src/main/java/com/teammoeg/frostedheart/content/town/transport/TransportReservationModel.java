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

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Forge-independent formulas and comparison rules for transport-capacity reservations. */
public final class TransportReservationModel {
    private static final int COMPARISON_ULPS = 8;

    private TransportReservationModel() {
    }

    public static double warehouseWeightedDistance(
            BlockPos endpointPos,
            Collection<WarehouseTopologyEntry> warehouses
    ) {
        if (endpointPos == null || warehouses == null || warehouses.isEmpty()) {
            return Double.NaN;
        }
        List<WarehouseTopologyEntry> sorted = new ArrayList<>(warehouses.size());
        double maximumWeight = 0.0;
        for (WarehouseTopologyEntry warehouse : warehouses) {
            if (warehouse == null || !Double.isFinite(warehouse.capacityWeight())
                    || warehouse.capacityWeight() <= 0.0) {
                return Double.NaN;
            }
            sorted.add(warehouse);
            maximumWeight = Math.max(maximumWeight, warehouse.capacityWeight());
        }
        if (!Double.isFinite(maximumWeight) || maximumWeight <= 0.0) {
            return Double.NaN;
        }
        sorted.sort(WarehouseTopologyEntry.CORE_POS_ORDER);

        double numerator = 0.0;
        double denominator = 0.0;
        for (WarehouseTopologyEntry warehouse : sorted) {
            BlockPos corePos = warehouse.corePos();
            long dx = Math.abs((long) endpointPos.getX() - corePos.getX());
            long dy = Math.abs((long) endpointPos.getY() - corePos.getY());
            long dz = Math.abs((long) endpointPos.getZ() - corePos.getZ());
            double distance = (double) (dx + dy + dz);
            double normalizedWeight = warehouse.capacityWeight() / maximumWeight;
            numerator += normalizedWeight * distance;
            denominator += normalizedWeight;
            if (!isFiniteNonNegative(numerator) || !isFiniteNonNegative(denominator)) {
                return Double.NaN;
            }
        }
        if (denominator <= 0.0) {
            return Double.NaN;
        }
        double result = numerator / denominator;
        return isFiniteNonNegative(result) ? result : Double.NaN;
    }

    public static double warehouseDistanceFactor(double scaleMetric, TransportConsumerParameters parameters) {
        if (!isFiniteNonNegative(scaleMetric) || parameters == null) {
            return Double.NaN;
        }
        double factor = 1.0 + parameters.warehouseDistanceCostPerBlock() * scaleMetric;
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
            case WAREHOUSE_INTERFACE -> warehouseDistanceFactor(scaleMetric, parameters);
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
