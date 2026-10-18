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

/** Read-only aggregate derived from the current capacity resource and reservation map. */
public record TownTransportSummary(
        double totalCapacity,
        double reservedCapacity,
        double remainingRegistrableCapacity,
        double shortfall,
        double effectiveRateScale
) {
    public static TownTransportSummary from(double totalCapacity, TownTransportState state) {
        double total = TransportReservationModel.isFiniteNonNegative(totalCapacity) ? totalCapacity : 0.0;
        double reserved = state == null ? 0.0 : state.getReservedTransportCapacity();
        return new TownTransportSummary(
                total,
                reserved,
                Math.max(0.0, total - reserved),
                Math.max(0.0, reserved - total),
                TransportReservationModel.effectiveRateScale(total, reserved));
    }
}
