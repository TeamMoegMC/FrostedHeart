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
