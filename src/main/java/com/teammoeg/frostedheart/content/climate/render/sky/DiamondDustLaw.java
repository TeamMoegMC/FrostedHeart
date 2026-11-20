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

package com.teammoeg.frostedheart.content.climate.render.sky;

import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;

/**
 * The extinction of diamond dust that the weather calls for, the ice crystals in the clear, calm, cold air after snow.
 * There is none while it snows. After the snow ends the extinction rises to FULL_EXTINCTION over RISE_HOURS, holds
 * until HOLD_END_HOURS, and falls back to none at END_HOURS, evenly between those points.
 * The extinction is per kilometer at the ground, with the diffracted light counted as transmitted, as for the ice
 * crystal layer. Hours are climate hours, and snow includes storms.
 */
public final class DiamondDustLaw {
    public static final double FULL_EXTINCTION = 1.0;
    public static final double RISE_HOURS = 1.0;
    public static final double HOLD_END_HOURS = 12.0;
    /**
     * The client knows the snow of the last WorldClimate.SNOW_SEARCH_HOURS only.
     */
    public static final double END_HOURS = WorldClimate.SNOW_SEARCH_HOURS;
    /**
     * Climate seconds over which smoothedExtinction closes all but 1/e of the gap to the target, one climate hour.
     */
    public static final double SMOOTHING_SECONDS = 50.0;

    private DiamondDustLaw() {
    }

    /**
     * The extinction the weather calls for.
     * @param hoursSinceSnow hours since the last snow ended, or positive infinity when there was none
     */
    public static double targetExtinction(boolean snowing, double hoursSinceSnow) {
        if (snowing || !(hoursSinceSnow < END_HOURS)) {
            return 0.0;
        }
        double hours = Math.max(0.0, hoursSinceSnow);
        if (hours < RISE_HOURS) {
            return FULL_EXTINCTION * hours / RISE_HOURS;
        }
        if (hours <= HOLD_END_HOURS) {
            return FULL_EXTINCTION;
        }
        return FULL_EXTINCTION * (END_HOURS - hours) / (END_HOURS - HOLD_END_HOURS);
    }

    /**
     * Moves an extinction toward the target over the given climate seconds, so that a jump of the target or of the
     * clock becomes a glide of about SMOOTHING_SECONDS.
     */
    public static double smoothedExtinction(double current, double target, double elapsedSeconds) {
        if (elapsedSeconds <= 0.0) {
            return current;
        }
        double kept = Math.exp(-elapsedSeconds / SMOOTHING_SECONDS);
        return target + (current - target) * kept;
    }
}
