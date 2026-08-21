/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.util.Optional;

/** Safe numeric payload for one morning transport-capacity shortage notice. */
public record TownTransportShortageNotice(
        double totalCapacity,
        double reservedCapacity,
        double shortfall,
        double effectiveRateScale
) {
    public TownTransportShortageNotice {
        if (!TransportReservationModel.isFiniteNonNegative(totalCapacity)
                || !TransportReservationModel.isFiniteNonNegative(reservedCapacity)
                || !TransportReservationModel.isFiniteNonNegative(shortfall)
                || !TransportReservationModel.isFiniteNonNegative(effectiveRateScale)
                || effectiveRateScale > 1.0
                || !TransportReservationModel.meaningfullyGreater(reservedCapacity, totalCapacity)) {
            throw new IllegalArgumentException("Transport shortage notice contains invalid numeric fields.");
        }
        double expectedShortfall = reservedCapacity - totalCapacity;
        double expectedScale = TransportReservationModel.effectiveRateScale(
                totalCapacity, reservedCapacity);
        if (!nearlyEqual(shortfall, expectedShortfall)
                || !nearlyEqual(effectiveRateScale, expectedScale)) {
            throw new IllegalArgumentException("Transport shortage notice contains inconsistent derived fields.");
        }
    }

    public static Optional<TownTransportShortageNotice> from(
            double totalCapacity, double reservedCapacity
    ) {
        if (!TransportReservationModel.meaningfullyGreater(reservedCapacity, totalCapacity)) {
            return Optional.empty();
        }
        return Optional.of(new TownTransportShortageNotice(
                totalCapacity,
                reservedCapacity,
                reservedCapacity - totalCapacity,
                TransportReservationModel.effectiveRateScale(totalCapacity, reservedCapacity)));
    }

    private static boolean nearlyEqual(double first, double second) {
        return Double.doubleToLongBits(first) == Double.doubleToLongBits(second)
                || Math.abs(first - second)
                <= TransportReservationModel.comparisonTolerance(first, second);
    }
}
