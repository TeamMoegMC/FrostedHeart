/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.render.weather;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.WhiteCurtainVisualProfile;

/** Immutable V1 work caps selected explicitly by the player. */
public enum WeatherQualityProfile {
    FAST(4, 8, 3, 12, 256, 12, WhiteCurtainVisualProfile.FAST),
    FANCY(8, 4, 5, 20, 1024, 32, WhiteCurtainVisualProfile.FANCY);

    private final int gridRadius;
    private final int gridSpacingBlocks;
    private final int wallSlices;
    private final int wallSegments;
    private final int precipitationColumns;
    private final int terrainQueriesPerTick;
    private final WhiteCurtainVisualProfile fieldProfile;

    WeatherQualityProfile(int gridRadius, int gridSpacingBlocks, int wallSlices, int wallSegments,
                          int precipitationColumns, int terrainQueriesPerTick,
                          WhiteCurtainVisualProfile fieldProfile) {
        this.gridRadius = gridRadius;
        this.gridSpacingBlocks = gridSpacingBlocks;
        this.wallSlices = wallSlices;
        this.wallSegments = wallSegments;
        this.precipitationColumns = precipitationColumns;
        this.terrainQueriesPerTick = terrainQueriesPerTick;
        this.fieldProfile = fieldProfile;
    }

    public int gridRadius() {
        return gridRadius;
    }

    public int gridSpacingBlocks() {
        return gridSpacingBlocks;
    }

    public int gridSide() {
        return gridRadius * 2 + 1;
    }

    public int wallSlices() {
        return wallSlices;
    }

    public int wallSegments() {
        return wallSegments;
    }

    public int precipitationColumns() {
        return precipitationColumns;
    }

    public int terrainQueriesPerTick() {
        return terrainQueriesPerTick;
    }

    public WhiteCurtainVisualProfile fieldProfile() {
        return fieldProfile;
    }
}
