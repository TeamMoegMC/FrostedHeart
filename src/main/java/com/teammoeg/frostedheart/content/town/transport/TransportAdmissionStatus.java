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

/** Stable persisted state of one admitted endpoint. */
public enum TransportAdmissionStatus {
    ACTIVE,
    DISABLED,
    REDSTONE_PAUSED,
    UNAVAILABLE;

    public static final Codec<TransportAdmissionStatus> CODEC = Codec.STRING.comapFlatMap(
            value -> Arrays.stream(values())
                    .filter(status -> status.name().equals(value))
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown transport admission status: " + value)),
            TransportAdmissionStatus::name);
}
