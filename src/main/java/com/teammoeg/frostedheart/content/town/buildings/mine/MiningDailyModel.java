/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.buildings.mine;

/**
 * Forge-independent arithmetic for one mine-base work settlement.
 * <p>
 * The game supplies runtime FHConfig values; the command-line simulator
 * supplies the corresponding TownModelParameters values.
 */
public final class MiningDailyModel {
    private MiningDailyModel() {
    }

    /** Total item-resource output requested by all eligible miners. */
    public static double requestedOutput(double totalMiningSwe, double outputPerSweDay) {
        return nonNegative(totalMiningSwe) * nonNegative(outputPerSweDay);
    }

    /** Allocates a continuous item-resource amount by the current recipe weights. */
    public static double weightedShare(double totalAmount, double weight, double totalWeight) {
        double safeTotal = nonNegative(totalAmount);
        double safeWeight = nonNegative(weight);
        double safeWeightTotal = nonNegative(totalWeight);
        return safeWeightTotal > 0.0 ? safeTotal * safeWeight / safeWeightTotal : 0.0;
    }

    /** Current dynamic vacancy priority for one mining base. */
    /** @deprecated Replaced by the ordered town-level staffing plan. */
    @Deprecated(forRemoval = true)
    public static double assignmentPriority(
            int currentWorkers,
            int maximumWorkers,
            double basePriority,
            double penaltyPerWorker,
            double fillRatioBonus
    ) {
        int maximum = Math.max(0, maximumWorkers);
        int current = Math.max(0, currentWorkers);
        if (maximum == 0 || current >= maximum) return Double.NEGATIVE_INFINITY;
        return finiteOrZero(basePriority)
                - nonNegative(penaltyPerWorker) * current
                + finiteOrZero(fillRatioBonus) * current / maximum;
    }

    /** Number of mine chunks that have been completely exhausted. */
    public static long exhaustedChunks(double cumulativeOreUnits, double oreReservePerChunk) {
        double reserve = positive(oreReservePerChunk, "oreReservePerChunk");
        return (long) Math.floor(nonNegative(cumulativeOreUnits) / reserve);
    }

    /** Number of mine chunks entered when exhausted chunks can be replaced immediately. */
    public static long enteredChunks(double cumulativeOreUnits, double oreReservePerChunk) {
        double ore = nonNegative(cumulativeOreUnits);
        if (ore <= 0.0) return 0L;
        double reserve = positive(oreReservePerChunk, "oreReservePerChunk");
        return (long) Math.ceil(ore / reserve);
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
        return value;
    }
}
