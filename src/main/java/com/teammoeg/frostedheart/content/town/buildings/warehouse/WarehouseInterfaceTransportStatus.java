/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;

/** Finite server-owned transport state shown by the warehouse interface menu. */
public enum WarehouseInterfaceTransportStatus {
    UNBOUND,
    WAREHOUSE_UNAVAILABLE,
    ACTIVE,
    THROTTLED,
    DISABLED;

    public static final Codec<WarehouseInterfaceTransportStatus> CODEC = Codec.STRING.comapFlatMap(
            value -> Arrays.stream(values())
                    .filter(status -> status.name().equals(value))
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown warehouse interface transport status: " + value)),
            WarehouseInterfaceTransportStatus::name);
}
