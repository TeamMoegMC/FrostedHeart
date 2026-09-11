/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingDecision;
import com.teammoeg.frostedheart.content.town.transport.P2PBindingState;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/** Complete bounded server-authoritative view for the terminal menu. */
public record P2PTerminalMenuView(
        P2PTerminalRole role,
        P2PTerminalVisualState visualState,
        List<P2PTerminalConnectionView> connections,
        P2PFilterSnapshot sendFilter,
        P2PFilterSnapshot receiveFilter,
        int maximumRateItemsPerSecond,
        double townTotalCapacity,
        double townRemainingCapacity,
        P2PBindingDecision lastDecision,
        double requiredAdditionalCapacity
) {
    public static final int MAX_VISIBLE_CONNECTIONS = P2PBindingState.MAX_DIRECTED_BINDINGS;
    private static final Codec<List<P2PTerminalConnectionView>> CONNECTIONS_CODEC =
            P2PTerminalConnectionView.CODEC.listOf().flatXmap(
                    P2PTerminalMenuView::validateConnections,
                    P2PTerminalMenuView::validateConnections);
    public static final Codec<P2PTerminalMenuView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            P2PTerminalRole.CODEC.fieldOf("role").forGetter(P2PTerminalMenuView::role),
            StringRepresentable.fromEnum(P2PTerminalVisualState::values)
                    .fieldOf("visualState").forGetter(P2PTerminalMenuView::visualState),
            CONNECTIONS_CODEC.optionalFieldOf("connections", List.of())
                    .forGetter(P2PTerminalMenuView::connections),
            P2PFilterSnapshot.CODEC.fieldOf("sendFilter").forGetter(P2PTerminalMenuView::sendFilter),
            P2PFilterSnapshot.CODEC.fieldOf("receiveFilter").forGetter(P2PTerminalMenuView::receiveFilter),
            Codec.INT.fieldOf("maximumRateItemsPerSecond")
                    .forGetter(P2PTerminalMenuView::maximumRateItemsPerSecond),
            Codec.DOUBLE.fieldOf("townTotalCapacity").forGetter(P2PTerminalMenuView::townTotalCapacity),
            Codec.DOUBLE.fieldOf("townRemainingCapacity").forGetter(P2PTerminalMenuView::townRemainingCapacity),
            P2PBindingDecision.CODEC.fieldOf("lastDecision").forGetter(P2PTerminalMenuView::lastDecision),
            Codec.DOUBLE.fieldOf("requiredAdditionalCapacity")
                    .forGetter(P2PTerminalMenuView::requiredAdditionalCapacity)
    ).apply(instance, P2PTerminalMenuView::new));

    public static final P2PTerminalMenuView EMPTY = new P2PTerminalMenuView(
            P2PTerminalRole.SHIPPING, P2PTerminalVisualState.UNBOUND, List.of(),
            new P2PFilterSnapshot(true, false, List.of()),
            new P2PFilterSnapshot(true, false, List.of()),
            TownModelParameters.Defaults.TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND,
            0.0, 0.0, P2PBindingDecision.INVALID_REQUEST, 0.0);

    public P2PTerminalMenuView {
        connections = connections == null ? List.of() : List.copyOf(connections);
        if (role == null || visualState == null || sendFilter == null || receiveFilter == null
                || lastDecision == null || maximumRateItemsPerSecond <= 0
                || !TransportReservationModel.isFiniteNonNegative(townTotalCapacity)
                || !TransportReservationModel.isFiniteNonNegative(townRemainingCapacity)
                || !TransportReservationModel.isFiniteNonNegative(requiredAdditionalCapacity)) {
            throw new IllegalArgumentException("Invalid P2P terminal menu view.");
        }
    }

    private static DataResult<List<P2PTerminalConnectionView>> validateConnections(
            List<P2PTerminalConnectionView> connections
    ) {
        return connections != null && connections.size() <= MAX_VISIBLE_CONNECTIONS
                ? DataResult.success(connections)
                : DataResult.error(() -> "P2P menu view exceeds "
                + MAX_VISIBLE_CONNECTIONS + " connections.");
    }
}
