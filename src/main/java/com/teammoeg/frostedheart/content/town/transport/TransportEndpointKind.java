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
import com.mojang.serialization.DataResult;

import java.util.Arrays;

/** Identifies how an endpoint supplies its scale metric and moves items. */
public enum TransportEndpointKind {
    WAREHOUSE_INTERFACE,
    /** One sender-owned reservation for a direct, same-dimension P2P link. */
    P2P_DIRECT_LINK;

    public static final Codec<TransportEndpointKind> CODEC = Codec.STRING.comapFlatMap(
            value -> Arrays.stream(values())
                    .filter(kind -> kind.name().equals(value))
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown transport endpoint kind: " + value)),
            TransportEndpointKind::name);
}
