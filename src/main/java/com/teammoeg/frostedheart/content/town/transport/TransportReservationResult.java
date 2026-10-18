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
