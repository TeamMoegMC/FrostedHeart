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

import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Turns a sun elevation into the day time at which the overworld's sun stands there. Every elevation occurs twice a
 * day, once as the sun sets and once as it rises, so the branch picks one.
 * The sun's elevation is asin(cos(2 pi T)), with T from DimensionType.timeOfDay, and T in turn is
 * (2 d + 1/2 - cos(pi d) / 2) / 3 for the fraction d of the day after day time 6000.
 */
public final class SunElevationTimes {
    private static final int BISECTION_STEPS = 64;

    public enum Branch {
        SETTING("setting"),
        RISING("rising");

        private final String key;

        Branch(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        @Nullable
        public static Branch byKey(String key) {
            for (Branch branch : values()) {
                if (branch.key.equals(key)) {
                    return branch;
                }
            }
            return null;
        }
    }

    private SunElevationTimes() {
    }

    /**
     * The day time from 0 to 23999 that puts the sun nearest the elevation on the branch. Elevations outside the range
     * from -90 to 90 degrees are clamped to it.
     */
    public static long dayTime(double elevationDegrees, Branch branch) {
        double elevation = Math.max(-90.0, Math.min(90.0, elevationDegrees));
        double timeOfDay = branch == Branch.SETTING ? (90.0 - elevation) / 360.0 : (270.0 + elevation) / 360.0;
        double target = 3.0 * timeOfDay - 0.5;
        double low = 0.0;
        double high = 1.0;
        for (int step = 0; step < BISECTION_STEPS; step++) {
            double middle = (low + high) * 0.5;
            if (2.0 * middle - Math.cos(Math.PI * middle) * 0.5 < target) {
                low = middle;
            } else {
                high = middle;
            }
        }
        double dayFraction = (low + high) * 0.5;
        long time = Math.round(Level.TICKS_PER_DAY * (dayFraction + 0.25));
        return Math.floorMod(time, Level.TICKS_PER_DAY);
    }
}
