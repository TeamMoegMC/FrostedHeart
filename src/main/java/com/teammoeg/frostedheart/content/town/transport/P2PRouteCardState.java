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
