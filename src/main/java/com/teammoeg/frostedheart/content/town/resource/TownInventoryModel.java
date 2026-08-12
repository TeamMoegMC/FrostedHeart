/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;

/**
 * Forge-independent warehouse mutation rules shared by gameplay and the
 * command-line town simulator.
 */
public final class TownInventoryModel {
    private TownInventoryModel() {
    }

    /**
     * Calculates the amount actually modified by ATTEMPT/MAXIMIZE.
     * ATTEMPT is all-or-nothing; MAXIMIZE accepts the available amount.
     */
    public static double modifiedAmount(
            double requestedAmount,
            double availableAmount,
            ResourceActionMode mode
    ) {
        double requested = nonNegative(requestedAmount);
        double available = nonNegativeOrInfinity(availableAmount);
        if (requested <= available) return requested;
        if (mode == ResourceActionMode.MAXIMIZE
                && available > TownFoodInventoryModel.RESOURCE_EPSILON) {
            return available;
        }
        return 0.0;
    }

    public static Mutation settle(
            double requestedAmount,
            double availableAmount,
            ResourceActionMode mode
    ) {
        double requested = nonNegative(requestedAmount);
        double modified = modifiedAmount(requested, availableAmount, mode);
        return new Mutation(
                requested,
                modified,
                Math.max(0.0, requested - modified),
                modified + TownFoodInventoryModel.RESOURCE_EPSILON >= requested);
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double nonNegativeOrInfinity(double value) {
        if (value == Double.POSITIVE_INFINITY) return value;
        return nonNegative(value);
    }

    public record Mutation(
            double requestedAmount,
            double modifiedAmount,
            double residualAmount,
            boolean fullyApplied
    ) {
    }
}
