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

/**
 * The ice optical depth that the weather calls for, as cirrostratus thickens ahead of snow.
 * On clear days the depth rises with the day's humidity from CLEAR_MINIMUM_DEPTH to CLEAR_MAXIMUM_DEPTH.
 * Over the RAMP_HOURS before snow it rises through the depths of RAMP_DEPTHS at the hours of RAMP_HOURS_BEFORE_SNOW
 * to SNOW_DEPTH when the snow starts, stays there while it snows, and falls back to the clear day depth over the
 * RECOVERY_HOURS after the snow ends. Between those points the depth changes evenly in its logarithm.
 * Hours are climate hours, and snow includes storms.
 */
public final class IceWeatherLaw {
    public static final double CLEAR_MINIMUM_DEPTH = 0.02;
    public static final double CLEAR_MAXIMUM_DEPTH = 0.15;
    /**
     * The highest humidity of WorldClimate's days.
     */
    public static final double MAXIMUM_HUMIDITY = 50.0;
    public static final double SNOW_DEPTH = 3.0;
    public static final double RAMP_HOURS = 24.0;
    public static final double RECOVERY_HOURS = 6.0;
    /**
     * Hours before snow at which the depth reaches the matching entry of RAMP_DEPTHS, after the clear day depth at
     * RAMP_HOURS.
     */
    public static final double[] RAMP_HOURS_BEFORE_SNOW = {12.0, 6.0, 0.0};
    public static final double[] RAMP_DEPTHS = {0.4, 1.0, SNOW_DEPTH};
    /**
     * Climate seconds over which smoothedDepth closes all but 1/e of the gap to the target, one climate hour.
     */
    public static final double SMOOTHING_SECONDS = 50.0;

    private IceWeatherLaw() {
    }

    public static double clearDepth(double humidity) {
        double fraction = Math.max(0.0, Math.min(1.0, humidity / MAXIMUM_HUMIDITY));
        return CLEAR_MINIMUM_DEPTH + (CLEAR_MAXIMUM_DEPTH - CLEAR_MINIMUM_DEPTH) * fraction;
    }

    /**
     * The depth the weather calls for.
     * @param hoursUntilSnow hours until the next snow starts, or positive infinity when none is coming
     * @param hoursSinceSnow hours since the last snow ended, or positive infinity when there was none
     */
    public static double targetDepth(boolean snowing, double hoursUntilSnow, double hoursSinceSnow, double humidity) {
        if (snowing) {
            return SNOW_DEPTH;
        }
        double clear = clearDepth(humidity);
        double depth = Math.max(clear, rampDepth(clear, Math.max(0.0, hoursUntilSnow)));
        double sinceSnow = Math.max(0.0, hoursSinceSnow);
        if (sinceSnow < RECOVERY_HOURS) {
            depth = Math.max(depth, logarithmicBlend(SNOW_DEPTH, clear, sinceSnow / RECOVERY_HOURS));
        }
        return depth;
    }

    private static double rampDepth(double clear, double hoursUntilSnow) {
        if (hoursUntilSnow >= RAMP_HOURS) {
            return clear;
        }
        double laterHours = RAMP_HOURS;
        double laterDepth = clear;
        for (int point = 0; point < RAMP_HOURS_BEFORE_SNOW.length; point++) {
            double pointHours = RAMP_HOURS_BEFORE_SNOW[point];
            if (hoursUntilSnow >= pointHours) {
                double fraction = (laterHours - hoursUntilSnow) / (laterHours - pointHours);
                return logarithmicBlend(laterDepth, RAMP_DEPTHS[point], fraction);
            }
            laterHours = pointHours;
            laterDepth = RAMP_DEPTHS[point];
        }
        return SNOW_DEPTH;
    }

    private static double logarithmicBlend(double from, double to, double fraction) {
        return from * Math.pow(to / from, fraction);
    }

    /**
     * Moves a depth toward the target over the given climate seconds, evenly in its logarithm, so that a jump of the
     * target or of the clock becomes a glide of about SMOOTHING_SECONDS. Both depths must be positive.
     */
    public static double smoothedDepth(double current, double target, double elapsedSeconds) {
        if (elapsedSeconds <= 0.0) {
            return current;
        }
        double kept = Math.exp(-elapsedSeconds / SMOOTHING_SECONDS);
        return logarithmicBlend(target, current, kept);
    }
}
