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

/** Immutable input shared by transport-capacity consumers and their pure models. */
public record TransportConsumerParameters(
        int defaultRateItemsPerSecond,
        int minimumRateItemsPerSecond,
        int maximumRateItemsPerSecond,
        double warehouseDistanceCostPerBlock,
        double p2pDistanceCostPerBlock
) {
    public TransportConsumerParameters {
        if (defaultRateItemsPerSecond < 0) {
            throw new IllegalArgumentException("defaultRateItemsPerSecond must be non-negative.");
        }
        if (minimumRateItemsPerSecond <= 0) {
            throw new IllegalArgumentException("minimumRateItemsPerSecond must be positive.");
        }
        if (maximumRateItemsPerSecond < minimumRateItemsPerSecond) {
            throw new IllegalArgumentException("maximumRateItemsPerSecond must not be below the minimum.");
        }
        if (defaultRateItemsPerSecond > maximumRateItemsPerSecond) {
            throw new IllegalArgumentException("defaultRateItemsPerSecond exceeds the maximum.");
        }
        if (!Double.isFinite(warehouseDistanceCostPerBlock) || warehouseDistanceCostPerBlock < 0.0) {
            throw new IllegalArgumentException("warehouseDistanceCostPerBlock must be finite and non-negative.");
        }
        if (!Double.isFinite(p2pDistanceCostPerBlock) || p2pDistanceCostPerBlock < 0.0) {
            throw new IllegalArgumentException("p2pDistanceCostPerBlock must be finite and non-negative.");
        }
    }

    /** Zero disables an endpoint; every non-zero setting must fit the configured range. */
    public boolean isRateValid(int rateItemsPerSecond) {
        return rateItemsPerSecond == 0
                || (rateItemsPerSecond >= minimumRateItemsPerSecond
                && rateItemsPerSecond <= maximumRateItemsPerSecond);
    }
}
