/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Maximum physical and learning activity completed by one resident during a town day.
 *
 * <p>Both components use the range {@code 0..1}. Multiple completed jobs combine by taking the
 * component-wise maximum, so repeated settlement cannot create more than one full activity day.</p>
 */
public record ResidentActivity(double physical, double learning) {
    public static final ResidentActivity NONE = new ResidentActivity(0.0, 0.0);
    public static final Codec<ResidentActivity> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.DOUBLE.optionalFieldOf("physical", 0.0)
                            .forGetter(ResidentActivity::physical),
                    Codec.DOUBLE.optionalFieldOf("learning", 0.0)
                            .forGetter(ResidentActivity::learning)
            ).apply(instance, ResidentActivity::new));

    public ResidentActivity {
        physical = unit(physical);
        learning = unit(learning);
    }

    /** Returns the component-wise maximum of this activity and another completed job. */
    public ResidentActivity max(ResidentActivity other) {
        if (other == null) return this;
        return new ResidentActivity(
                Math.max(physical, other.physical),
                Math.max(learning, other.learning));
    }

    private static double unit(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
