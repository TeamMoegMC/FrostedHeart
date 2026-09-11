/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

/** Structured outcome of one authoritative P2P connection transaction. */
public enum P2PBindingDecision {
    ACCEPTED,
    INVALID_REQUEST,
    INVALID_ENDPOINT,
    INCOMPATIBLE_ENDPOINTS,
    CROSS_DIMENSION,
    SELF_LINK,
    INSUFFICIENT_CAPACITY,
    STALE_CONNECTION;

    public static final Codec<P2PBindingDecision> CODEC = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown P2P binding decision: " + name);
        }
    }, decision -> decision.name().toLowerCase(Locale.ROOT));
}
