/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure numerical functions used by both {@link GeneratorData} and the town
 * model audit. This class deliberately models process ticks, not an invented
 * fuel-value unit.
 */
public final class GeneratorFuelModel {
    private GeneratorFuelModel() {
    }

    /**
     * Scales recipe process ticks using decimal arithmetic, then applies the
     * generator's explicit round-down rule. Decimal arithmetic prevents a
     * nominal multiplier such as 0.7 + 0.2 from becoming one tick shorter
     * because of its binary floating-point representation.
     */
    public static int effectiveFuelProcessTicks(
            int recipeProcessTicks,
            double baseDurationMultiplier,
            double researchEfficiencyBonus
    ) {
        if (recipeProcessTicks < 0
                || !Double.isFinite(baseDurationMultiplier)
                || !Double.isFinite(researchEfficiencyBonus)
                || baseDurationMultiplier < 0.0
                || researchEfficiencyBonus < 0.0) {
            throw new IllegalArgumentException("Fuel duration inputs must be finite and non-negative.");
        }
        BigDecimal multiplier = BigDecimal.valueOf(baseDurationMultiplier)
                .add(BigDecimal.valueOf(researchEfficiencyBonus));
        return BigDecimal.valueOf(recipeProcessTicks)
                .multiply(multiplier)
                .setScale(0, RoundingMode.FLOOR)
                .intValueExact();
    }

    /**
     * Returns whether another fuel item is needed to pay a requested batch.
     * An exactly sufficient balance must be spent before another item loads.
     */
    public static boolean shouldLoadNextFuel(int remainingProcessTicks, int requestedProcessTicks) {
        return remainingProcessTicks < requestedProcessTicks;
    }

    /**
     * Adds a newly loaded item's process ticks without discarding the balance
     * left by the previous item.
     */
    public static int addFuelProcessTicks(int remainingProcessTicks, int loadedProcessTicks) {
        if (remainingProcessTicks < 0) {
            throw new IllegalArgumentException("remainingProcessTicks must be non-negative.");
        }
        requirePositive(loadedProcessTicks, "loadedProcessTicks");
        return Math.addExact(remainingProcessTicks, loadedProcessTicks);
    }

    /**
     * Ideal continuous-use rate that assumes every effective process tick is
     * consumed. This is the historical algebraic baseline in town-model.md.
     */
    public static double idealFuelItemsPerDay(
            int effectiveFuelProcessTicks,
            int processTicksPerGameTick,
            int gameTicksPerDay
    ) {
        requirePositive(effectiveFuelProcessTicks, "effectiveFuelProcessTicks");
        requirePositive(processTicksPerGameTick, "processTicksPerGameTick");
        requirePositive(gameTicksPerDay, "gameTicksPerDay");
        return (double) gameTicksPerDay * processTicksPerGameTick / effectiveFuelProcessTicks;
    }

    /**
     * Long-run rate under GeneratorData.townTick batching. Fuel balances are
     * carried across item and batch boundaries, so batching does not change
     * the ideal recipe-duration rate.
     */
    public static double currentTownBatchFuelItemsPerDay(
            int effectiveFuelProcessTicks,
            int processTicksPerGameTick,
            int batchGameTicks,
            int gameTicksPerDay
    ) {
        int processTicksPerBatch = Math.multiplyExact(processTicksPerGameTick, batchGameTicks);
        requirePositive(effectiveFuelProcessTicks, "effectiveFuelProcessTicks");
        requirePositive(processTicksPerBatch, "processTicksPerBatch");
        requirePositive(gameTicksPerDay, "gameTicksPerDay");
        return idealFuelItemsPerDay(
                effectiveFuelProcessTicks,
                processTicksPerGameTick,
                gameTicksPerDay);
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
