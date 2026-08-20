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
 */

package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum TownProductionStopReason {
    NONE,
    NO_ELIGIBLE_WORKERS,
    NO_USABLE_MINES,
    TERRAIN_DEPLETED,
    MISSING_LOOT_TABLE,
    ACCUMULATING,
    BUILDING_UNWORKABLE,
    OUTPUT_DISABLED,
    RESOURCE_REJECTED;

    public static final Codec<TownProductionStopReason> CODEC = Codec.STRING.xmap(
            TownProductionStopReason::fromSerializedName,
            reason -> reason.name().toLowerCase(Locale.ROOT)
    );

    private static TownProductionStopReason fromSerializedName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
