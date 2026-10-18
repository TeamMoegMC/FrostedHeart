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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Complete result of one P2P bind, rebind, or unbind transaction. */
public record P2PBindingResult(
        P2PBindingDecision decision,
        Optional<UUID> connectionId,
        Set<UUID> replacedConnectionIds,
        double requiredAdditionalCapacity,
        TownTransportSummary townSummaryAfter
) {
    public P2PBindingResult {
        connectionId = connectionId == null ? Optional.empty() : connectionId;
        replacedConnectionIds = Collections.unmodifiableSet(new LinkedHashSet<>(
                replacedConnectionIds == null ? Set.of() : replacedConnectionIds));
        requiredAdditionalCapacity = TransportReservationModel.isFiniteNonNegative(
                requiredAdditionalCapacity) ? requiredAdditionalCapacity : 0.0;
    }
}
