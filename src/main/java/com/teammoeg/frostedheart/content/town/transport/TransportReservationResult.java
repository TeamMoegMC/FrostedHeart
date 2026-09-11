/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.util.Optional;

/** Complete result of one authoritative endpoint mutation. */
public record TransportReservationResult(
        TransportReservationDecision decision,
        Optional<TransportReservation> reservationAfter,
        TownTransportSummary townSummaryAfter,
        double requiredAdditionalCapacity
) {
    public TransportReservationResult {
        reservationAfter = reservationAfter == null ? Optional.empty() : reservationAfter;
        requiredAdditionalCapacity = TransportReservationModel.isFiniteNonNegative(requiredAdditionalCapacity)
                ? requiredAdditionalCapacity
                : 0.0;
    }
}
