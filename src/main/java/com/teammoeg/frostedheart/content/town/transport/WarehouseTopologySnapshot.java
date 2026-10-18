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

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Immutable server-side view of every effective warehouse in one town. */
public record WarehouseTopologySnapshot(
        ResourceKey<Level> townDimension,
        List<WarehouseTopologyEntry> entries
) {
    public static final WarehouseTopologySnapshot UNAVAILABLE =
            new WarehouseTopologySnapshot(null, List.of());

    public WarehouseTopologySnapshot {
        List<WarehouseTopologyEntry> sorted = new ArrayList<>(
                entries == null ? List.of() : entries);
        sorted.sort(WarehouseTopologyEntry.CORE_POS_ORDER);
        entries = List.copyOf(sorted);
    }

    public static WarehouseTopologySnapshot of(
            ResourceKey<Level> townDimension,
            Collection<WarehouseTopologyEntry> entries
    ) {
        return new WarehouseTopologySnapshot(townDimension,
                entries == null ? List.of() : List.copyOf(entries));
    }

    public boolean isUsable() {
        return townDimension != null && !entries.isEmpty();
    }
}
