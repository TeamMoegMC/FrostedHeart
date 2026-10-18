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
