/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;

/** Pure presentation metrics shared by capacity-style virtual-resource pages. */
public final class TownVirtualResourceMetrics {
    private TownVirtualResourceMetrics() {
    }

    public record CapacityBreakdown(
            double total,
            double used,
            double available,
            double shortfall,
            double utilizationFraction,
            double effectiveRateScale
    ) {
        public boolean overcommitted() {
            return shortfall > TeamTownResourceHolder.DELTA;
        }
    }

    public static CapacityBreakdown capacity(double total, double used) {
        double safeTotal = nonNegative(total);
        double safeUsed = nonNegative(used);
        double difference = safeTotal - safeUsed;
        double utilization = safeTotal > TeamTownResourceHolder.DELTA
                ? safeUsed / safeTotal
                : safeUsed > TeamTownResourceHolder.DELTA ? 1.0 : 0.0;
        double effectiveRateScale = safeUsed > TeamTownResourceHolder.DELTA
                ? Math.min(1.0, safeTotal / safeUsed)
                : 1.0;
        return new CapacityBreakdown(
                safeTotal,
                safeUsed,
                Math.max(0.0, difference),
                Math.max(0.0, -difference),
                finiteOrZero(utilization),
                finiteOrZero(effectiveRateScale));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
