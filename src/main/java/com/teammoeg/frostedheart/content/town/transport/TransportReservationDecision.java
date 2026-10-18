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

import java.util.Arrays;

/** Structured outcome returned by the server-authoritative reservation facade. */
public enum TransportReservationDecision {
    ACCEPTED,
    INSUFFICIENT_CAPACITY,
    INVALID_REQUEST,
    INVALID_BINDING;

    public static final Codec<TransportReservationDecision> CODEC = Codec.STRING.comapFlatMap(
            value -> Arrays.stream(values())
                    .filter(decision -> decision.name().equals(value))
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown transport reservation decision: " + value)),
            TransportReservationDecision::name);
}
