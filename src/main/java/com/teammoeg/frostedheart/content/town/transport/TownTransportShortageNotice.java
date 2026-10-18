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
 *
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
