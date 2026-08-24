/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persistent state carried by a freight route card. It is never binding authority. */
public record P2PRouteCardState(
        Optional<P2PTerminalEndpoint> selectedEndpoint,
        Optional<UUID> connectionId
) {
    public static final P2PRouteCardState EMPTY = new P2PRouteCardState(
            Optional.empty(), Optional.empty());

    private static final Codec<P2PRouteCardState> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            P2PTerminalEndpoint.CODEC.optionalFieldOf("selectedEndpoint")
                    .forGetter(P2PRouteCardState::selectedEndpoint),
            UUIDUtil.CODEC.optionalFieldOf("connectionId")
                    .forGetter(P2PRouteCardState::connectionId)
    ).apply(instance, P2PRouteCardState::new));

    public static final Codec<P2PRouteCardState> CODEC = RAW_CODEC.flatXmap(
            P2PRouteCardState::validate, P2PRouteCardState::validate);

    public P2PRouteCardState {
        selectedEndpoint = selectedEndpoint == null ? Optional.empty() : selectedEndpoint;
        connectionId = connectionId == null ? Optional.empty() : connectionId;
        if (selectedEndpoint.isPresent() && connectionId.isPresent()) {
            throw new IllegalArgumentException(
                    "A route card cannot hold a selection and connection simultaneously.");
        }
    }

    public static P2PRouteCardState selected(P2PTerminalEndpoint endpoint) {
        return new P2PRouteCardState(Optional.of(Objects.requireNonNull(endpoint, "endpoint")),
                Optional.empty());
    }

    public static P2PRouteCardState connected(UUID connectionId) {
        return new P2PRouteCardState(Optional.empty(),
                Optional.of(Objects.requireNonNull(connectionId, "connectionId")));
    }

    private static DataResult<P2PRouteCardState> validate(P2PRouteCardState state) {
        if (state.selectedEndpoint().isPresent() && state.connectionId().isPresent()) {
            return DataResult.error(() ->
                    "A route card cannot hold a selection and connection simultaneously.");
        }
        return DataResult.success(state);
    }
}
