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
 * The light the calculated sky is drawn for, the sun or the moon, with its elevation, its direction, and a scale and a
 * color on its light. The sun moves in the plane of x and y, and the moon stands exactly opposite it.
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
    private final double[] color = {1.0, 1.0, 1.0};

    /**
     * Sets the light from the angle of the time of day, which is 2 pi times DimensionType.timeOfDay, with a white color.
     */
    public void set(Kind kind, double timeOfDayAngle, double scale) {
        set(kind, timeOfDayAngle, scale, 1.0, 1.0, 1.0);
    }

    /**
     * Sets the light as the other form does, with a color that multiplies each channel of its radiance.
     */
    public void set(Kind kind, double timeOfDayAngle, double scale, double red, double green, double blue) {
        double sunElevation = Math.asin(Math.cos(timeOfDayAngle));
        float sunX = (float) -Math.sin(timeOfDayAngle);
        float sunY = (float) Math.cos(timeOfDayAngle);
        float sunSide = Math.sin(timeOfDayAngle) > 0.0 ? -1.0F : 1.0F;
        this.kind = kind;
        this.scale = scale;
        color[0] = red;
        color[1] = green;
        color[2] = blue;
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

    /**
     * The color of the light in a channel, which multiplies its radiance along with the scale.
     */
    public double color(int channel) {
        return color[channel];
    }

    /**
     * The factor on a channel of the radiance of a sky built for the given kind of light. It is 0 while the sky was
     * built for the other kind, since its views are for the other light's elevation.
     */
    public double radianceScale(int channel, Kind builtFor) {
        if (builtFor != kind) {
            return 0.0;
        }
        return scale * color[channel];
    }

    /**
     * The factor on a channel of the ground radiance for the terrain light, which only the sun gives, so at night the
     * terrain keeps vanilla's night light.
     */
    public double terrainScale(int channel, Kind builtFor) {
        if (kind != Kind.SUN) {
            return 0.0;
        }
        return radianceScale(channel, builtFor);
    }
}
