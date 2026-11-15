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
 * The light the calculated sky is drawn for, the sun or the moon, with its elevation, its direction, and a scale on
 * its light. The sun moves in the plane of x and y, and the moon stands exactly opposite it.
 * One instance is reused and set again each frame.
 */
public final class SkyLight {
    public enum Kind {
        SUN,
        MOON
    }

    private Kind kind = Kind.SUN;
    private double elevationRadians;
    private float x;
    private float y;
    private float side = 1.0F;
    private double scale = 1.0;

    /**
     * Sets the light from the angle of the time of day, which is 2 pi times DimensionType.timeOfDay.
     */
    public void set(Kind kind, double timeOfDayAngle, double scale) {
        double sunElevation = Math.asin(Math.cos(timeOfDayAngle));
        float sunX = (float) -Math.sin(timeOfDayAngle);
        float sunY = (float) Math.cos(timeOfDayAngle);
        float sunSide = Math.sin(timeOfDayAngle) > 0.0 ? -1.0F : 1.0F;
        this.kind = kind;
        this.scale = scale;
        if (kind == Kind.SUN) {
            elevationRadians = sunElevation;
            x = sunX;
            y = sunY;
            side = sunSide;
        } else {
            elevationRadians = -sunElevation;
            x = -sunX;
            y = -sunY;
            side = -sunSide;
        }
    }

    public Kind kind() {
        return kind;
    }

    public double elevationRadians() {
        return elevationRadians;
    }

    /**
     * The x component of the direction to the light.
     */
    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    /**
     * 1 or -1, which turns world azimuths into azimuths from the light as the tables use them.
     */
    public float side() {
        return side;
    }

    /**
     * The factor on the light's radiance before the tone map.
     */
    public double scale() {
        return scale;
    }
}
