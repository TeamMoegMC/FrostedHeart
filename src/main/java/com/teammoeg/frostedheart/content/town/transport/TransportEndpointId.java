/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
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
