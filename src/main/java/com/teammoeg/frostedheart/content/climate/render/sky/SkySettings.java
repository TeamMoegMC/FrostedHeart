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
 * A value for every SkySetting, from the client config or given by a GameTest. Instances never change.
 * Optical depths are vertical and measured at 550 nm. The spread is the geometric standard deviation of particle
 * diameter. The texel size is the side of the sky's square texels in degrees, and 0 draws a smooth sky.
 * A build reads only the settings that rebuild something, and tables use the ice optical depth of their grid depth.
 */
public final class SkySettings {
    private static final SkySetting[] SETTINGS = SkySetting.values();
    public static final SkySettings DEFAULT = defaults();

    private final double[] values;

    private SkySettings(double[] values) {
        this.values = values;
    }

    private static SkySettings defaults() {
        double[] values = new double[SETTINGS.length];
        for (SkySetting setting : SETTINGS) {
            values[setting.ordinal()] = setting.defaultValue();
        }
        return new SkySettings(values);
    }

    /**
     * The settings the client config holds.
     */
    public static SkySettings fromConfig() {
        double[] values = new double[SETTINGS.length];
        for (SkySetting setting : SETTINGS) {
            values[setting.ordinal()] = setting.get();
        }
        return new SkySettings(values);
    }

    /**
     * Sets the client config to these settings without saving the config file.
     */
    public void applyToConfig() {
        for (SkySetting setting : SETTINGS) {
            setting.set(values[setting.ordinal()]);
        }
    }

    public double get(SkySetting setting) {
        return values[setting.ordinal()];
    }

    public SkySettings with(SkySetting setting, double value) {
        double[] changed = values.clone();
        changed[setting.ordinal()] = value;
        return new SkySettings(changed);
    }

    public SkySettings withIceOpticalDepth(double depth) {
        return with(SkySetting.ICE_OPTICAL_DEPTH, depth);
    }

    /**
     * Whether every setting that rebuilds the part has the same value in both.
     */
    boolean samePart(SkySettings other, SkySetting.Rebuilds part) {
        for (SkySetting setting : SETTINGS) {
            if (setting.rebuilds() == part && values[setting.ordinal()] != other.values[setting.ordinal()]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a build gives the same result for both, because every setting that rebuilds something is the same.
     */
    boolean sameBuild(SkySettings other) {
        return samePart(other, SkySetting.Rebuilds.TABLES) && samePart(other, SkySetting.Rebuilds.HALOS);
    }

    /**
     * Whether every setting that rebuilds something has the value the client config holds, without allocating.
     */
    boolean matchesConfig() {
        for (SkySetting setting : SETTINGS) {
            if (setting.rebuilds() != SkySetting.Rebuilds.NOTHING && values[setting.ordinal()] != setting.get()) {
                return false;
            }
        }
        return true;
    }

    public double nanobotOpticalDepth() {
        return get(SkySetting.NANOBOT_OPTICAL_DEPTH);
    }

    public double nanobotMedianNanometers() {
        return get(SkySetting.NANOBOT_MEDIAN_NANOMETERS);
    }

    public double nanobotSpread() {
        return get(SkySetting.NANOBOT_SPREAD);
    }

    public double iceOpticalDepth() {
        return get(SkySetting.ICE_OPTICAL_DEPTH);
    }

    public double texelDegrees() {
        return get(SkySetting.TEXEL_DEGREES);
    }

    /**
     * Texels per edge of each face of the cube map that holds the sky texels, or 0 for a smooth sky.
     */
    public int texelFaceSize() {
        double texelDegrees = texelDegrees();
        if (texelDegrees <= 0.0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(90.0 / texelDegrees));
    }

    /**
     * The texel size that the cube map really has, close to the configured one, or 0 for a smooth sky.
     */
    public double actualTexelDegrees() {
        int faceSize = texelFaceSize();
        return faceSize > 0 ? 90.0 / faceSize : 0.0;
    }
}
