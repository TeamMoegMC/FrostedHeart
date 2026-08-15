/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.building;

/** A town building whose saved state exposes a player-relevant effective temperature. */
public interface ITownTemperatureBuilding {
    double getEffectiveTemperature();
}
