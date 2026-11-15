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

import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;

/**
 * The settings of the calculated sky in the client config group CalculatedSky, with their keys, comments, defaults,
 * and ranges, and what a build does again when one changes. FHConfig defines them all with define, in this order.
 * The command, the status, SkySettings, and the screenshot harness read this table, so a new setting is one constant.
 * Where settings are held as numbers, a switch is 1 when on and 0 when off.
 */
public enum SkySetting {
    EXPOSURE("exposure", "Display exposure of the calculated sky.", Rebuilds.NOTHING, 6.0, 0.5, 50.0),
    NANOBOT_OPTICAL_DEPTH("nanobotOpticalDepth", "Vertical optical depth of the nanobot layer at 550 nm. 0.25 is "
            + "about 30 megatons worldwide at the default size.", Rebuilds.TABLES, 0.25, 0.0, 4.0),
    NANOBOT_MEDIAN_NANOMETERS("nanobotMedianNanometers", "Median nanobot diameter in nanometers.", Rebuilds.TABLES,
            100.0, 50.0, 1000.0),
    NANOBOT_SPREAD("nanobotSpread", "Geometric standard deviation of nanobot diameter. Narrow sizes give colored "
            + "skies, wide sizes give white haze.", Rebuilds.TABLES, 1.75, 1.05, 3.0),
    /**
     * IceDepthGrid ends at its maximum.
     */
    ICE_OPTICAL_DEPTH("iceOpticalDepth", "Vertical optical depth of the ice crystal layer, which also draws the "
            + "halos, sun dogs, and arcs. Used while iceFollowsWeather is off.", Rebuilds.NOTHING, 0.1, 0.0, 3.0),
    ICE_FOLLOWS_WEATHER("iceFollowsWeather", "Lets the ice crystal layer follow the weather, thin on clear days and "
            + "thickening over the day before snow.", true, ICE_OPTICAL_DEPTH),
    DUST_EXTINCTION("dustExtinction", "Extinction per kilometer of the diamond dust near the ground, the ice crystals "
            + "in the clear air after snow. Used while dustFollowsWeather is off.", Rebuilds.NOTHING, 0.0, 0.0, 3.0),
    DUST_FOLLOWS_WEATHER("dustFollowsWeather", "Lets the diamond dust follow the weather, forming after each snow and "
            + "gone a climate day later.", true, DUST_EXTINCTION),
    MOONLIGHT_BRIGHTNESS("moonlightBrightness", "Irradiance of the full moon as a share of the sun's, with which the "
            + "moon lights the sky at night. Other phases are dimmer. The real full moon gives 0.0000025, too little to "
            + "show.", Rebuilds.NOTHING, 0.0125, 0.0, 0.2),
    MOONLIGHT_BLUE_SHIFT("moonlightBlueShift", "Color of moonlight, from the moon's own faintly red light at 0 to the "
            + "blue that eyes adapted to the dark see at 1.", Rebuilds.NOTHING, 0.5, 0.0, 1.0),
    MOONLIGHT_SWITCH_DEGREES("moonlightSwitchDegrees", "Sun elevation in degrees below which the moon lights the sky "
            + "instead of the sun. The sun's light fades out over the 3 degrees above it.", Rebuilds.NOTHING, -9.5,
            -18.0, -2.0),
    MOONLIGHT_FADE_DEGREES("moonlightFadeDegrees", "Degrees the sun sinks below moonlightSwitchDegrees while the "
            + "moon's light fades in, and rises before dawn while it fades out.", Rebuilds.NOTHING, 4.0, 0.0, 30.0),
    PILLAR_BRIGHTNESS("pillarBrightness", "Brightness of light pillars, as a factor on their calibrated brightness.",
            Rebuilds.NOTHING, 0.3, 0.0, 10.0),
    PILLAR_NEAR_FADE("pillarNearFade", "Distance in blocks from the eye beyond which ice crystals fully show light "
            + "pillars. Nearer crystals fade out down to a quarter of it, since too few of them line up to show a "
            + "smooth pillar. 0 shows them all.", Rebuilds.NOTHING, 8.0, 0.0, 64.0),
    PILLAR_CRYSTAL_SIZE("pillarCrystalSize", "Diameter in micrometers of the plate crystals that show light pillars. "
            + "Each pixel of a pillar shows only the crystals that line up in it, so near pillars glitter and far ones "
            + "look smooth. Larger crystals are fewer and glitter more. 0 draws smooth pillars.", Rebuilds.NOTHING,
            65.0, 0.0, 1000.0),
    PILLAR_TWINKLE_SECONDS("pillarTwinkleSeconds", "Seconds over which the glittering crystals of light pillars change "
            + "while the eye stands still. Moving the eye changes them faster.", Rebuilds.NOTHING, 0.25, 0.02, 5.0),
    PILLAR_TEXEL_DEGREES("pillarTexelDegrees", "Size in degrees of the square cells that light pillars are drawn in. "
            + "Each pillar has its own grid standing on its light, so the cells stay on the pillar as the eye moves. "
            + "0 draws pillars per pixel.", Rebuilds.NOTHING, 0.25, 0.0, 2.0),
    PILLAR_FADE_IN_SIGMAS("pillarFadeInSigmas", "Height of the fade in at the bottom of light pillars, in standard "
            + "deviations of the tilt of their plate crystals, so pillars of wobbling plates fade in over a longer "
            + "way. 0 starts them on a line.", Rebuilds.NOTHING, 2.0, 0.0, 8.0),
    PILLAR_BASE_SHARE("pillarBaseShare", "Share of their light that light pillars show at the bottom of their fade "
            + "in, rising to all of it at its top, on top of the fade in itself.", Rebuilds.NOTHING, 0.5, 0.0, 1.0),
    PILLAR_DEPTH_BAND("pillarDepthBand", "Distance in blocks on each side of a light over which the scene in front "
            + "of it hides its light pillar. Nearer scene hides the pillar, farther scene shows it.", Rebuilds.NOTHING,
            0.5, 0.0, 4.0),
    DUST_GLITTER_BRIGHTNESS("dustGlitterBrightness", "Brightness of the glints of diamond dust near the eye, as a "
            + "factor on their calibrated brightness. 0 draws none.", Rebuilds.NOTHING, 2.0, 0.0, 10.0),
    DUST_GLITTER_PLAIN_SHARE("dustGlitterPlainShare", "Share of the glints of diamond dust that show in directions "
            + "away from the halos. On the 22 degree halo, the sun dogs, and the pillars all of them show.",
            Rebuilds.NOTHING, 0.2, 0.0, 1.0),
    DUST_GLITTER_FALL_SPEED("dustGlitterFallSpeed", "Speed in blocks per second at which the glints of diamond dust "
            + "fall, give or take 30%, each within 30 degrees of straight down.", Rebuilds.NOTHING, 2.0, 0.0, 3.0),
    DUST_GLITTER_COUNT("dustGlitterCount", "Points drawn for the glints of diamond dust in a cube of 32 blocks around "
            + "the eye. Each lights a falling crystal for a moment once a life, in a new place each time. Those "
            + "within 16 blocks show where the dust is dense. 0 draws none.", Rebuilds.NOTHING, 32768.0,
            0.0, 65536.0),
    DUST_GLITTER_SIZE("dustGlitterSize", "Width in pixels at half its peak of the core of a glint of diamond dust 6 "
            + "blocks away, in a window 1080 pixels high. Larger windows scale it. Bright glints saturate and show "
            + "wider.", Rebuilds.NOTHING, 3.5, 0.5, 8.0),
    DUST_GLITTER_SIZE_SPREAD("dustGlitterSizeSpread", "How much glints of diamond dust grow nearer the eye and shrink "
            + "farther away. Their width goes as 6 blocks over their distance to this power, from a quarter to 4 "
            + "times dustGlitterSize, with their light spread over it. 0 draws all at the same size.",
            Rebuilds.NOTHING, 1.5, 0.0, 2.0),
    DUST_GLITTER_LIT_SECONDS("dustGlitterLitSeconds", "Median time in seconds a glint of diamond dust stays lit. Lit "
            + "times spread from 0.6 to 3.5 times it, and each crystal's life lasts 5 to 10 times it.",
            Rebuilds.NOTHING, 1.0, 0.2, 4.0),
    TEXEL_DEGREES("texelDegrees", "Size of the sky's square texels in degrees. They are fixed to the sky like the "
            + "sun's texture. 0 draws a smooth sky.", Rebuilds.HALOS, 0.5, 0.0, 5.0);

    /**
     * What a build does again when a setting changes. The views are built again with either part.
     */
    public enum Rebuilds {
        /**
         * Nothing, because the setting is read every frame.
         */
        NOTHING,
        /**
         * The tables of every ice depth.
         */
        TABLES,
        /**
         * The halo profile and the halo maps, which are blurred by the texel size.
         */
        HALOS
    }

    private final String key;
    private final String comment;
    private final Rebuilds rebuilds;
    private final boolean isSwitch;
    private final double defaultValue;
    private final double minimum;
    private final double maximum;
    @Nullable
    private final SkySetting manualValue;
    @Nullable
    private ForgeConfigSpec.DoubleValue numberValue;
    @Nullable
    private ForgeConfigSpec.BooleanValue switchValue;

    SkySetting(String key, String comment, Rebuilds rebuilds, double defaultValue, double minimum, double maximum) {
        this.key = key;
        this.comment = comment;
        this.rebuilds = rebuilds;
        this.isSwitch = false;
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
        this.manualValue = null;
    }

    /**
     * A switch, which is read every frame. While it is off, the manual value takes its place.
     */
    SkySetting(String key, String comment, boolean defaultOn, @Nullable SkySetting manualValue) {
        this.key = key;
        this.comment = comment;
        this.rebuilds = Rebuilds.NOTHING;
        this.isSwitch = true;
        this.defaultValue = defaultOn ? 1.0 : 0.0;
        this.minimum = 0.0;
        this.maximum = 1.0;
        this.manualValue = manualValue;
    }

    /**
     * Defines every setting in the config the builder builds, in the order of the constants.
     */
    public static void define(ForgeConfigSpec.Builder builder) {
        for (SkySetting setting : values()) {
            builder.comment(setting.comment);
            if (setting.isSwitch) {
                setting.switchValue = builder.define(setting.key, setting.defaultValue != 0.0);
            } else {
                setting.numberValue = builder.defineInRange(setting.key, setting.defaultValue, setting.minimum,
                        setting.maximum);
            }
        }
    }

    /**
     * The value in the client config.
     */
    public double get() {
        if (isSwitch) {
            return switchValue.get() ? 1.0 : 0.0;
        }
        return numberValue.get();
    }

    /**
     * Whether the switch is on in the client config.
     */
    public boolean isOn() {
        return get() != 0.0;
    }

    /**
     * Sets the value in the client config without saving the config file.
     */
    public void set(double value) {
        if (isSwitch) {
            switchValue.set(value != 0.0);
        } else {
            numberValue.set(value);
        }
    }

    /**
     * Saves the client config file, which writes every value in it.
     */
    public void save() {
        if (isSwitch) {
            switchValue.save();
        } else {
            numberValue.save();
        }
    }

    public String key() {
        return key;
    }

    public Rebuilds rebuilds() {
        return rebuilds;
    }

    public boolean isSwitch() {
        return isSwitch;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    /**
     * The setting used in place of this switch while it is off, or null.
     */
    @Nullable
    public SkySetting manualValue() {
        return manualValue;
    }
}
