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
import net.minecraft.core.GlobalPos;

import java.util.Comparator;
import java.util.Objects;

/** Stable endpoint identity. The position is the physical consumer block, not its warehouse core. */
public record TransportEndpointId(GlobalPos endpointPos) {
    public static final Codec<TransportEndpointId> CODEC = GlobalPos.CODEC.xmap(
            TransportEndpointId::new, TransportEndpointId::endpointPos);

    public static final Comparator<TransportEndpointId> STABLE_COMPARATOR = Comparator
            .comparing((TransportEndpointId id) -> id.endpointPos.dimension().location().toString())
            .thenComparingInt(id -> id.endpointPos.pos().getX())
            .thenComparingInt(id -> id.endpointPos.pos().getY())
            .thenComparingInt(id -> id.endpointPos.pos().getZ());

    public TransportEndpointId {
        Objects.requireNonNull(endpointPos, "endpointPos");
    }
}
