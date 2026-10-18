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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.field;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Provider-scoped identity, independent of field geometry and runtime generation. */
public record ThermalFieldKey(ResourceLocation provider, long ownerHigh, long ownerLow, int channel)
        implements Comparable<ThermalFieldKey> {
    public static ThermalFieldKey of(ResourceLocation provider, UUID owner, int channel) {
        return new ThermalFieldKey(provider, owner.getMostSignificantBits(), owner.getLeastSignificantBits(), channel);
    }

    @Override
    public int compareTo(ThermalFieldKey other) {
        int result = provider.compareTo(other.provider);
        if (result == 0) result = Long.compare(ownerHigh, other.ownerHigh);
        if (result == 0) result = Long.compare(ownerLow, other.ownerLow);
        return result == 0 ? Integer.compare(channel, other.channel) : result;
    }
}
