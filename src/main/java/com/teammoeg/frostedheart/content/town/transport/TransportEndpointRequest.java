/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import net.minecraft.core.GlobalPos;

import java.util.Objects;

/** Untrusted endpoint-setting inputs; reserved capacity is always derived by the server. */
public record TransportEndpointRequest(
        TransportEndpointId endpointId,
        TransportEndpointKind endpointKind,
        GlobalPos boundWarehouseCorePos,
        int rateItemsPerSecond,
        double scaleMetric
) {
    public TransportEndpointRequest {
        Objects.requireNonNull(endpointId, "endpointId");
        Objects.requireNonNull(endpointKind, "endpointKind");
        Objects.requireNonNull(boundWarehouseCorePos, "boundWarehouseCorePos");
    }
}
