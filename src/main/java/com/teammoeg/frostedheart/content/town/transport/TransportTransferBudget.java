/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Runtime-only transport scheduling state shared by warehouse interfaces and P2P senders.
 *
 * <p>The configured item rate decides the fastest allowed attempt interval, while the
 * effective town-scaled rate only generates transfer tokens. Tokens are capped at one
 * second of effective capacity (at most 64 items) and never create an unbounded catch-up
 * burst after idle, blocked, or backoff periods.
 */
public final class TransportTransferBudget {
    public static final int SERVER_TICKS_PER_SECOND = 20;
    public static final int MAX_BATCH_ITEMS = 64;
    public static final int MAX_INTERVAL_TICKS = 80;

    private static final BigDecimal TICKS_PER_SECOND = BigDecimal.valueOf(SERVER_TICKS_PER_SECOND);

    private int configuredRate;
    private int baseIntervalTicks = SERVER_TICKS_PER_SECOND;
    private int currentIntervalTicks = SERVER_TICKS_PER_SECOND;
    private long nextAttemptTick = Long.MIN_VALUE;
    private long lastAllowanceTick = Long.MIN_VALUE;
    private BigDecimal tokens = BigDecimal.ZERO;
    private boolean clearTokensOnNextSuccess;

    /** Creates a fresh budget. Call {@link #configure(int)} before use with a real rate. */
    public TransportTransferBudget() {
    }

    /**
     * Sets the player-configured item rate and rebuilds interval state when the rate changes.
     *
     * <p>Only the configured rate changes the base interval; effective-rate scaling never
     * alters the fastest allowed attempt cadence.
     */
    public void configure(int rateItemsPerSecond) {
        int newBase = baseIntervalTicksFor(rateItemsPerSecond);
        if (rateItemsPerSecond == configuredRate && newBase == baseIntervalTicks) {
            return;
        }
        configuredRate = rateItemsPerSecond;
        baseIntervalTicks = newBase;
        reset();
    }

    /** Maps a configured rate to the fastest allowed attempt interval in server ticks. */
    public static int baseIntervalTicksFor(int rateItemsPerSecond) {
        if (rateItemsPerSecond <= 32) {
            return 20;
        }
        if (rateItemsPerSecond <= 64) {
            return 10;
        }
        if (rateItemsPerSecond <= 128) {
            return 5;
        }
        if (rateItemsPerSecond <= 640) {
            return 2;
        }
        return 1;
    }

    /** Returns the configured-rate base interval in server ticks. */
    public int baseIntervalTicks() {
        return baseIntervalTicks;
    }

    /** Returns the current adaptive interval in server ticks. */
    public int currentIntervalTicks() {
        return currentIntervalTicks;
    }

    /** Returns true while the current interval is longer than the base interval. */
    public boolean isBackingOff() {
        return currentIntervalTicks > baseIntervalTicks;
    }

    /** Returns the currently available fractional token balance in items. */
    public double getTokens() {
        return tokens.doubleValue();
    }

    /**
     * Adds allowance for the elapsed server ticks and returns the current one-run budget.
     *
     * <p>Returns {@code 0} when the block is not due, the effective rate is not positive,
     * or fewer than one whole item token is available. A zero return means the caller must
     * not touch external item capabilities.
     */
    public int beginAttempt(long gameTime, double effectiveRateItemsPerSecond) {
        addAllowance(gameTime, effectiveRateItemsPerSecond);
        if (gameTime < nextAttemptTick) {
            return 0;
        }
        if (!Double.isFinite(effectiveRateItemsPerSecond) || effectiveRateItemsPerSecond <= 0.0) {
            return 0;
        }
        int budget = Math.min(MAX_BATCH_ITEMS, tokens.intValue());
        return Math.max(0, budget);
    }

    /** Records a real attempt that moved at least one item. */
    public void recordSuccess(int movedItems, long gameTime) {
        if (movedItems <= 0) {
            return;
        }
        BigDecimal moved = BigDecimal.valueOf(movedItems);
        tokens = tokens.subtract(moved);
        if (tokens.signum() < 0) {
            tokens = BigDecimal.ZERO;
        }
        if (clearTokensOnNextSuccess) {
            tokens = BigDecimal.ZERO;
            clearTokensOnNextSuccess = false;
        }
        currentIntervalTicks = Math.max(baseIntervalTicks, ceilDiv(currentIntervalTicks, 2));
        nextAttemptTick = gameTime + currentIntervalTicks;
    }

    /** Records a real zero-move attempt caused by blocked or empty transfer conditions. */
    public void recordFailure(long gameTime) {
        currentIntervalTicks = Math.min(
                MAX_INTERVAL_TICKS,
                Math.max(baseIntervalTicks, currentIntervalTicks * 2));
        clearTokensOnNextSuccess = true;
        nextAttemptTick = gameTime + currentIntervalTicks;
    }

    /** Clears tokens and prevents offline allowance accumulation without changing backoff. */
    public void pause(long gameTime) {
        tokens = BigDecimal.ZERO;
        lastAllowanceTick = gameTime;
    }

    /** Clears tokens and schedules the next probe at the current interval without changing backoff. */
    public void defer(long gameTime) {
        pause(gameTime);
        nextAttemptTick = gameTime + currentIntervalTicks;
    }

    /** Makes the next scheduled attempt eligible immediately without clearing backoff. */
    public void wake(long gameTime) {
        if (nextAttemptTick > gameTime) {
            nextAttemptTick = gameTime;
        }
    }

    /** Fully resets runtime scheduling state to the configured base interval. */
    public void reset() {
        tokens = BigDecimal.ZERO;
        lastAllowanceTick = Long.MIN_VALUE;
        nextAttemptTick = Long.MIN_VALUE;
        currentIntervalTicks = baseIntervalTicks;
        clearTokensOnNextSuccess = false;
    }

    private void addAllowance(long gameTime, double effectiveRateItemsPerSecond) {
        if (lastAllowanceTick == Long.MIN_VALUE) {
            lastAllowanceTick = gameTime;
            nextAttemptTick = gameTime + currentIntervalTicks;
            return;
        }
        long elapsed = gameTime - lastAllowanceTick;
        lastAllowanceTick = gameTime;
        if (elapsed <= 0 || !Double.isFinite(effectiveRateItemsPerSecond)
                || effectiveRateItemsPerSecond <= 0.0) {
            return;
        }
        BigDecimal increment = BigDecimal.valueOf(effectiveRateItemsPerSecond)
                .multiply(BigDecimal.valueOf(elapsed))
                .divide(TICKS_PER_SECOND, MathContext.DECIMAL128);
        tokens = tokens.add(increment);
        int capacity = tokenCapacity(effectiveRateItemsPerSecond);
        if (tokens.compareTo(BigDecimal.valueOf(capacity)) > 0) {
            tokens = BigDecimal.valueOf(capacity);
        }
    }

    static int tokenCapacity(double effectiveRateItemsPerSecond) {
        if (!Double.isFinite(effectiveRateItemsPerSecond) || effectiveRateItemsPerSecond <= 0.0) {
            return 0;
        }
        double capacity = Math.ceil(effectiveRateItemsPerSecond);
        if (capacity < 1.0) {
            capacity = 1.0;
        }
        return (int) Math.min(MAX_BATCH_ITEMS, Math.max(1.0, capacity));
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
