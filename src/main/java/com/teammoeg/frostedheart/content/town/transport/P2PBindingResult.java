/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
