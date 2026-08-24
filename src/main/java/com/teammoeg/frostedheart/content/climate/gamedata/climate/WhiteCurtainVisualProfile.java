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

/** Fixed V1 field-quality profiles. Rendering work caps are defined by the client renderer. */
public enum WhiteCurtainVisualProfile {
    FAST(16.0F, 5.0F, 160.0F),
    FANCY(24.0F, 5.0F, 192.0F);

    private final float corridorEdgeFadeBlocks;
    private final float phaseTransitionSeconds;
    private final float minimumVisibilityBlocks;

    WhiteCurtainVisualProfile(float corridorEdgeFadeBlocks, float phaseTransitionSeconds,
                              float minimumVisibilityBlocks) {
        this.corridorEdgeFadeBlocks = corridorEdgeFadeBlocks;
        this.phaseTransitionSeconds = phaseTransitionSeconds;
        this.minimumVisibilityBlocks = minimumVisibilityBlocks;
    }

    public float corridorEdgeFadeBlocks() {
        return corridorEdgeFadeBlocks;
    }

    public float phaseTransitionSeconds() {
        return phaseTransitionSeconds;
    }

    public float minimumVisibilityBlocks() {
        return minimumVisibilityBlocks;
    }
}
