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

import net.minecraft.core.BlockPos;

import java.util.Comparator;
import java.util.Objects;

/**
 * 仓库拓扑条目：一座有效仓库的核心坐标与容量权重。
 * <p>
 * Forge-independent immutable entry of one usable warehouse in the town
 * warehouse topology: the warehouse core position and its capacity weight.
 * Weight validation is deliberately permissive here; callers that build
 * topology collections must exclude non-finite or non-positive weights, and
 * {@link TransportReservationModel#warehouseWeightedDistance} rejects such
 * entries with an undefined result instead of silently skipping them.
 *
 * @param corePos        仓库核心方块坐标 / warehouse core block position
 * @param capacityWeight 仓库容量权重 / warehouse capacity weight
 */
public record WarehouseTopologyEntry(BlockPos corePos, double capacityWeight) {
    /** 按 (x, y, z) 字典序的稳定排序，保证求和顺序与输入顺序无关。 / Stable (x, y, z) lexicographic order. */
    public static final Comparator<WarehouseTopologyEntry> CORE_POS_ORDER =
            Comparator.comparingLong((WarehouseTopologyEntry entry) -> entry.corePos().getX())
                    .thenComparingLong(entry -> entry.corePos().getY())
                    .thenComparingLong(entry -> entry.corePos().getZ());

    public WarehouseTopologyEntry {
        Objects.requireNonNull(corePos, "corePos");
        corePos = corePos.immutable();
    }
}
