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

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Flat diagnostic result of one daily strength or intelligence transition. */
public record ResidentAttributeChange(
        double nextValue,
        double effectiveActivity,
        double growth,
        double nutritionDecay,
        double ageDecay,
        double netChange
) {
    public static final ResidentAttributeChange EMPTY =
            new ResidentAttributeChange(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    public static final Codec<ResidentAttributeChange> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("nextValue", 0.0)
                            .forGetter(ResidentAttributeChange::nextValue),
                    Codec.DOUBLE.optionalFieldOf("effectiveActivity", 0.0)
                            .forGetter(ResidentAttributeChange::effectiveActivity),
                    Codec.DOUBLE.optionalFieldOf("growth", 0.0)
                            .forGetter(ResidentAttributeChange::growth),
                    Codec.DOUBLE.optionalFieldOf("nutritionDecay", 0.0)
                            .forGetter(ResidentAttributeChange::nutritionDecay),
                    Codec.DOUBLE.optionalFieldOf("ageDecay", 0.0)
                            .forGetter(ResidentAttributeChange::ageDecay),
                    Codec.DOUBLE.optionalFieldOf("netChange", 0.0)
                            .forGetter(ResidentAttributeChange::netChange)
            ).apply(instance, ResidentAttributeChange::new));

    public ResidentAttributeChange {
        nextValue = attribute(nextValue);
        effectiveActivity = unit(effectiveActivity);
        growth = nonNegative(growth);
        nutritionDecay = nonNegative(nutritionDecay);
        ageDecay = nonNegative(ageDecay);
        netChange = finite(netChange);
    }

    private static double attribute(double value) {
        return Math.max(0.0, Math.min(100.0, finite(value)));
    }

    private static double unit(double value) {
        return Math.max(0.0, Math.min(1.0, finite(value)));
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finite(value));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
