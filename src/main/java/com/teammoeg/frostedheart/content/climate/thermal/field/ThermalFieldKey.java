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
