/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.math.BigDecimal;
import java.math.MathContext;

/** Runtime-only fractional item budget. Unused whole-item budget is never retained. */
public final class TransportTransferBudget {
    public static final int SERVER_TICKS_PER_SECOND = 20;
    private static final BigDecimal TICKS_PER_SECOND = BigDecimal.valueOf(SERVER_TICKS_PER_SECOND);
    private static final BigDecimal MAX_BUDGET = BigDecimal.valueOf(Integer.MAX_VALUE);

    private BigDecimal transferRemainder = BigDecimal.ZERO;

    public int beginTick(double effectiveRateItemsPerSecond, boolean hasTransferDemand) {
        if (!hasTransferDemand) {
            return 0;
        }
        if (!Double.isFinite(effectiveRateItemsPerSecond) || effectiveRateItemsPerSecond <= 0.0) {
            return 0;
        }
        BigDecimal available = transferRemainder.add(
                BigDecimal.valueOf(effectiveRateItemsPerSecond)
                        .divide(TICKS_PER_SECOND, MathContext.DECIMAL128));
        if (available.compareTo(MAX_BUDGET) >= 0) {
            transferRemainder = BigDecimal.ZERO;
            return Integer.MAX_VALUE;
        }
        int budget = available.intValue();
        transferRemainder = available.subtract(BigDecimal.valueOf(budget));
        return budget;
    }

    public void reset() {
        transferRemainder = BigDecimal.ZERO;
    }

    public double getTransferRemainder() {
        return transferRemainder.doubleValue();
    }
}
