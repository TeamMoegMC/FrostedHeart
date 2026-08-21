/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
