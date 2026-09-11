/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;

/** Pending endpoint selection carried by a freight route card. */
public record P2PRouteCardState(
        Optional<P2PTerminalEndpoint> selectedEndpoint
) {
    public static final P2PRouteCardState EMPTY = new P2PRouteCardState(
            Optional.empty());

    public static final Codec<P2PRouteCardState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            P2PTerminalEndpoint.CODEC.optionalFieldOf("selectedEndpoint")
                    .forGetter(P2PRouteCardState::selectedEndpoint)
    ).apply(instance, P2PRouteCardState::new));

    public P2PRouteCardState {
        selectedEndpoint = selectedEndpoint == null ? Optional.empty() : selectedEndpoint;
    }

    public static P2PRouteCardState selected(P2PTerminalEndpoint endpoint) {
        return new P2PRouteCardState(Optional.of(
                Objects.requireNonNull(endpoint, "endpoint")));
    }
}
