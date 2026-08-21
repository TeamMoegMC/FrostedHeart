/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
