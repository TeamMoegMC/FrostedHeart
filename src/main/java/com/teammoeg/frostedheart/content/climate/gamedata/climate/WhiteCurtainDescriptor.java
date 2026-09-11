/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.math.Rect;
import net.minecraft.core.Direction;

/**
 * Persisted, sparse authority for one moving white curtain.
 *
 * <p>The field names in {@link #CODEC} are part of the existing world-save contract.</p>
 */
public record WhiteCurtainDescriptor(Rect affectedArea, Direction moveDirection, ClimateEvent climate) {
    public static final Codec<WhiteCurtainDescriptor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Rect.CODEC.fieldOf("area").forGetter(WhiteCurtainDescriptor::affectedArea),
            Direction.CODEC.fieldOf("move").forGetter(WhiteCurtainDescriptor::moveDirection),
            ClimateEventTrack.CODEC.fieldOf("climate").forGetter(WhiteCurtainDescriptor::climate)
    ).apply(instance, WhiteCurtainDescriptor::new));
    public static final Codec<java.util.List<WhiteCurtainDescriptor>> LIST_CODEC = Codec.list(CODEC);

    public WhiteCurtainDescriptor {
        if (!moveDirection.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("White curtain direction must be horizontal");
        }
    }
}
