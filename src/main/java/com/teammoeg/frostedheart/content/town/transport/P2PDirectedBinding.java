/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** One sender-owned direction within a stable player-created P2P connection. */
public record P2PDirectedBinding(
        UUID connectionId,
        P2PTerminalEndpoint sender,
        P2PTerminalEndpoint receiver,
        int rateItemsPerSecond,
        boolean redstonePaused,
        boolean senderRedstonePowered,
        boolean receiverRedstonePowered
) {
    public static final Codec<P2PDirectedBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("connectionId").forGetter(P2PDirectedBinding::connectionId),
            P2PTerminalEndpoint.CODEC.fieldOf("sender").forGetter(P2PDirectedBinding::sender),
            P2PTerminalEndpoint.CODEC.fieldOf("receiver").forGetter(P2PDirectedBinding::receiver),
            Codec.INT.fieldOf("rateItemsPerSecond").forGetter(P2PDirectedBinding::rateItemsPerSecond),
            Codec.BOOL.optionalFieldOf("redstonePaused", false)
                    .forGetter(P2PDirectedBinding::redstonePaused),
            Codec.BOOL.optionalFieldOf("senderRedstonePowered", false)
                    .forGetter(P2PDirectedBinding::senderRedstonePowered),
            Codec.BOOL.optionalFieldOf("receiverRedstonePowered", false)
                    .forGetter(P2PDirectedBinding::receiverRedstonePowered)
    ).apply(instance, P2PDirectedBinding::new));

    public static final Comparator<P2PDirectedBinding> STABLE_COMPARATOR = Comparator
            .comparing(P2PDirectedBinding::connectionId)
            .thenComparing(P2PDirectedBinding::sender, P2PTerminalEndpoint.STABLE_COMPARATOR)
            .thenComparing(P2PDirectedBinding::receiver, P2PTerminalEndpoint.STABLE_COMPARATOR);

    public P2PDirectedBinding {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(receiver, "receiver");
        if (sender.pos().equals(receiver.pos())) {
            throw new IllegalArgumentException("A P2P endpoint cannot bind to itself.");
        }
        if (!sender.pos().dimension().equals(receiver.pos().dimension())) {
            throw new IllegalArgumentException("P2P endpoints must share a dimension.");
        }
        if (!sender.role().canSend() || !receiver.role().canReceive()) {
            throw new IllegalArgumentException("P2P direction is incompatible with terminal roles.");
        }
        if (rateItemsPerSecond < 0) {
            throw new IllegalArgumentException("P2P rate must be non-negative.");
        }
        if (rateItemsPerSecond == 0) {
            redstonePaused = false;
        } else {
            // Old saves only have the aggregate flag; attribute it to the sender on migration.
            if (redstonePaused && !senderRedstonePowered && !receiverRedstonePowered) {
                senderRedstonePowered = true;
            }
            redstonePaused = senderRedstonePowered || receiverRedstonePowered;
        }
    }

    public P2PDirectedBinding(
            UUID connectionId,
            P2PTerminalEndpoint sender,
            P2PTerminalEndpoint receiver,
            int rateItemsPerSecond,
            boolean redstonePaused
    ) {
        this(connectionId, sender, receiver, rateItemsPerSecond, redstonePaused,
                redstonePaused, false);
    }

    public P2PDirectedBinding withRate(int rateItemsPerSecond) {
        return new P2PDirectedBinding(connectionId, sender, receiver,
                rateItemsPerSecond, redstonePaused && rateItemsPerSecond != 0,
                senderRedstonePowered, receiverRedstonePowered);
    }

    public P2PDirectedBinding withEndpointRedstonePowered(GlobalPos endpoint, boolean powered) {
        Objects.requireNonNull(endpoint, "endpoint");
        boolean senderPowered = senderRedstonePowered;
        boolean receiverPowered = receiverRedstonePowered;
        if (sender.pos().equals(endpoint)) {
            senderPowered = powered;
        } else if (receiver.pos().equals(endpoint)) {
            receiverPowered = powered;
        } else {
            return this;
        }
        return new P2PDirectedBinding(connectionId, sender, receiver,
                rateItemsPerSecond, senderPowered || receiverPowered,
                senderPowered, receiverPowered);
    }
}
