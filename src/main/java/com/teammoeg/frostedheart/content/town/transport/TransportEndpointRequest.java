/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import java.util.Objects;

/** Untrusted endpoint-setting inputs; reserved capacity is always derived by the server. */
public record TransportEndpointRequest(
        TransportEndpointId endpointId,
        TransportEndpointKind endpointKind,
        int rateItemsPerSecond
) {
    public TransportEndpointRequest {
        Objects.requireNonNull(endpointId, "endpointId");
        Objects.requireNonNull(endpointKind, "endpointKind");
    }
}
