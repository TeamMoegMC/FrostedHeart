/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/** One peer row in the terminal's related-endpoints page. */
public record P2PTerminalConnectionView(
        UUID connectionId,
        P2PTerminalEndpoint peer,
        int outgoingRateItemsPerSecond,
        int incomingRateItemsPerSecond,
        boolean peerLoaded,
        Optional<P2PFilterSnapshot> peerSendFilter,
        Optional<P2PFilterSnapshot> peerReceiveFilter
) {
    public static final Codec<P2PTerminalConnectionView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("connectionId").forGetter(P2PTerminalConnectionView::connectionId),
            P2PTerminalEndpoint.CODEC.fieldOf("peer").forGetter(P2PTerminalConnectionView::peer),
            Codec.INT.optionalFieldOf("outgoingRateItemsPerSecond", -1)
                    .forGetter(P2PTerminalConnectionView::outgoingRateItemsPerSecond),
            Codec.INT.optionalFieldOf("incomingRateItemsPerSecond", -1)
                    .forGetter(P2PTerminalConnectionView::incomingRateItemsPerSecond),
            Codec.BOOL.optionalFieldOf("peerLoaded", false)
                    .forGetter(P2PTerminalConnectionView::peerLoaded),
            P2PFilterSnapshot.CODEC.optionalFieldOf("peerSendFilter")
                    .forGetter(P2PTerminalConnectionView::peerSendFilter),
            P2PFilterSnapshot.CODEC.optionalFieldOf("peerReceiveFilter")
                    .forGetter(P2PTerminalConnectionView::peerReceiveFilter)
    ).apply(instance, P2PTerminalConnectionView::new));

    public P2PTerminalConnectionView {
        peerSendFilter = peerSendFilter == null ? Optional.empty() : peerSendFilter;
        peerReceiveFilter = peerReceiveFilter == null ? Optional.empty() : peerReceiveFilter;
    }
}
