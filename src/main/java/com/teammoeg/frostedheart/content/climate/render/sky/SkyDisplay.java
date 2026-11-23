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
 * How the calculated sky turns radiance into display values, as calculated_sky.fsh does. The tone curve is
 * 1 - exp(-exposure * radiance), and the result is encoded as sRGB.
 */
final class SkyDisplay {
    private SkyDisplay() {
    }

    /**
     * The tone mapped linear value of a radiance, from 0 to 1. Negative radiance counts as 0.
     */
    static double toneMap(double exposure, double radiance) {
        return 1.0 - Math.exp(-exposure * Math.max(0.0, radiance));
    }

    /**
     * The sRGB encoding of a linear value, clamped to the range from 0 to 1.
     */
    static double encodeSrgb(double linear) {
        double clamped = Math.max(0.0, Math.min(1.0, linear));
        if (clamped < 0.0031308) {
            return clamped * 12.92;
        }
        return 1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055;
    }

    /**
     * The linear value of an sRGB encoded value, clamped to the range from 0 to 1.
     */
    static double decodeSrgb(double encoded) {
        double clamped = Math.max(0.0, Math.min(1.0, encoded));
        if (clamped <= 0.04045) {
            return clamped / 12.92;
        }
        return Math.pow((clamped + 0.055) / 1.055, 2.4);
    }

    /**
     * The tone mapped value lifted by a floor, so that a radiance of 0 shows the floor and 1 stays 1.
     */
    static double withFloor(double toneMapped, double floor) {
        return 1.0 - (1.0 - toneMapped) * (1.0 - floor);
    }

    /**
     * The displayed value of a radiance, from 0 to 1.
     */
    static double displayValue(double exposure, double radiance) {
        return encodeSrgb(toneMap(exposure, radiance));
    }

    /**
     * The displayed value of a radiance in steps of an 8 bit channel, from 0 to 255.
     */
    static double displaySteps(double exposure, double radiance) {
        return 255.0 * displayValue(exposure, radiance);
    }
}
