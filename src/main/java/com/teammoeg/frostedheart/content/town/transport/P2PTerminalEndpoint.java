/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

import java.util.Comparator;
import java.util.Objects;

/** A terminal fact resolved from a loaded server-side endpoint. */
public record P2PTerminalEndpoint(GlobalPos pos, P2PTerminalRole role) {
    public static final Codec<P2PTerminalEndpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(P2PTerminalEndpoint::pos),
            P2PTerminalRole.CODEC.fieldOf("role").forGetter(P2PTerminalEndpoint::role)
    ).apply(instance, P2PTerminalEndpoint::new));

    public static final Comparator<P2PTerminalEndpoint> STABLE_COMPARATOR = Comparator
            .comparing((P2PTerminalEndpoint endpoint) -> new TransportEndpointId(endpoint.pos()),
                    TransportEndpointId.STABLE_COMPARATOR)
            .thenComparing(P2PTerminalEndpoint::role);

    public P2PTerminalEndpoint {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(role, "role");
    }

    public TransportEndpointId transportEndpointId() {
        return new TransportEndpointId(pos);
    }
}
