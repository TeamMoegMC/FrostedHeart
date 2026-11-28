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
 * The light pillar that plate crystals above a light show, a vertical line on the sky through the light.
 * Each point of it comes from the crystals at one height, which mirror the light to the eye.
 * Heights are relative to the eye, and distances and heights are in blocks.
 */
final class LightPillarOptics {
    /**
     * Nodes of the 8 point Gauss Legendre rule on the interval from -1 to 1.
     */
    private static final double[] NODES = {-0.9602898564975363, -0.7966664774136267, -0.5255324099163290,
            -0.1834346424956498, 0.1834346424956498, 0.5255324099163290, 0.7966664774136267, 0.9602898564975363};
    private static final double[] WEIGHTS = {0.1012285362903763, 0.2223810344533745, 0.3137066458778873,
            0.3626837833783620, 0.3626837833783620, 0.3137066458778873, 0.2223810344533745, 0.1012285362903763};
    /**
     * The tilts along the view ray that radiance integrates over, in standard deviations of the tilt.
     */
    private static final double TILT_RANGE_SIGMAS = 4.0;
    /**
     * Below this height difference in blocks, the optical depth of a path takes the density at its middle.
     */
    private static final double SMALLEST_RISE = 0.01;
    /**
     * Keeps crystals finitely far at the zenith, where the angle at the crystals between the eye and the light closes.
     */
    private static final double SMALLEST_SINE_AT_CRYSTAL = 1.0e-6;
    /**
     * The most standard deviations of the pillar's width that a ribbon covers on each side.
     */
    private static final double MOST_RIBBON_SPREADS = 8.0;
    /**
     * Crystals nearer to the eye than this share of the near fade do not count, since too few of them line up behind
     * each pixel to show as a band.
     */
    static final double NEAR_FADE_START_SHARE = 0.25;
    /**
     * The smallest value before sRGB encoding that a pillar adds to the display and is still drawn, which encodes to
     * half a step of 255.
     */
    static final double VISIBLE_VALUE = 0.5 / 255.0 / 12.92;
    /**
     * The smallest value before sRGB encoding at which a lone glint of a single pixel shows against the night sky,
     * which encodes to eight steps of 255. Ribbons hold glints from this value on and the smooth pillar from
     * VISIBLE_VALUE on.
     */
    static final double GLINT_VALUE = 8.0 / 255.0 / 12.92;
    /**
     * Ribbons end here, short of the zenith, where every pillar meets.
     */
    static final double HIGHEST_RIBBON_ELEVATION = Math.toRadians(89.5);
    /**
     * The lateral blur as a share of the angle of a pixel at the center of the screen.
     */
    private static final double PIXEL_BLUR_FRACTION = 0.5;
    /**
     * Turns the radiance of dustRadiance, times the face area per volume in 1 per block and the strength of the light,
     * into the value a pillar adds to the display before its tone curve. It makes the pillar of a torch 32 blocks away
     * and 1.12 blocks below the eye, whose dust bottom is 1.62 blocks below the eye, at an extinction of 1 per
     * kilometer, show 0.1 of the display in linear light at 10 degrees, with plates and wobbling plates of
     * crystals.csv and the lateral blur of a screen 1080 pixels tall with a vertical field of view of 70 degrees.
     */
    static final double BRIGHTNESS = 433.0;
    /**
     * The wavelength in blocks that spreads each crystal's reflection by diffraction.
     */
    private static final double WAVELENGTH = 550.0e-9;
    /**
     * Keeps the diffraction of a face seen nearly edge on finite.
     */
    private static final double SMALLEST_SINE_OF_GRAZING = 0.02;
    /**
     * The most that the crystals of a sky cell show, as a multiple of their mean value. Rarer and brighter glints show
     * at this value, so cells with fewer crystals than its inverse show less than their mean on average.
     */
    static final double MOST_SPARKLE = 64.0;
    /**
     * Mean counts of crystals above this are drawn from a normal deviate by a closed form of the Poisson distribution's
     * quantile instead of by summing the distribution.
     */
    private static final double LARGEST_COUNTED_MEAN = 3.0;
    private static final int MOST_DRAWN = 48;
    private static final int GOLDEN_RATIO_STEP = 0x9e3779b9;
    /**
     * Outside its ribbon, a pixel of a pillar shows the glint value with less than this chance per twinkle period.
     */
    static final double RIBBON_GLINT_CHANCE = 0.01;
    /**
     * Crystals count as showing the glint value from this share below it on, since the value of one crystal changes
     * along the pillar between the rows whose widths the ribbon takes, and the count it takes to reach the glint value
     * jumps where one crystal shows just that value.
     */
    private static final double RIBBON_GLINT_MARGIN = 0.1;
    /**
     * The standard normal quantile of RIBBON_GLINT_CHANCE.
     */
    private static final double RIBBON_GLINT_SIGMAS = 2.326;
    /**
     * The closed form fades out over this many blocks of the sheet's height below the cap, as
     * SHELTER_BAND in light_pillars.glsl.
     */
    static final double SHELTER_BAND = 0.5;
    /**
     * Stands in for a cap that is open, relative to the eye, in what LightPillarRenderer passes to light_pillars.glsl.
     */
    static final double OPEN_CAP_HEIGHT = 1.0e6;
    /**
     * Below this ratio of the half angle of a light's width to the spread across its pillar, the closed form takes a
     * Gaussian as wide as the smeared profile instead, which is within 0.2 percent of it at the peak, as SMEAR_SHARE in
     * light_pillars.glsl.
     */
    static final double SMEAR_SHARE = 0.5;
    /**
     * The coefficients of the complementary error function of Abramowitz and Stegun 7.1.26.
     */
    private static final double ERROR_FUNCTION_STEP = 0.3275911;
    private static final double[] ERROR_FUNCTION_COEFFICIENTS = {0.254829592, -0.284496736, 1.421413741,
            -1.453152027, 1.061405429};

    private LightPillarOptics() {
    }

    /**
     * The height of the crystals that show the point of the pillar at the elevation.
     */
    static double mirrorHeight(double elevation, double horizontalDistance, double lightHeight) {
        return 0.5 * (horizontalDistance * Math.tan(elevation) + lightHeight);
    }

    /**
     * The elevation of the point of the pillar that the crystals at the height show.
     */
    static double mirrorElevation(double crystalHeight, double horizontalDistance, double lightHeight) {
        return Math.atan2(2.0 * crystalHeight - lightHeight, horizontalDistance);
    }

    /**
     * The horizontal distance from the eye to the crystals at the height that mirror the light to the eye.
     */
    static double crystalDistance(double crystalHeight, double horizontalDistance, double lightHeight) {
        return horizontalDistance * crystalHeight / (2.0 * crystalHeight - lightHeight);
    }

    /**
     * The standard deviation across the pillar at the elevation, in radians on the sky, for plates whose tilt has
     * the standard deviation in radians.
     */
    static double lateralSpread(double elevation, double horizontalDistance, double lightHeight, double tiltSigma) {
        double crystalHeight = mirrorHeight(elevation, horizontalDistance, lightHeight);
        double eyeOverLightLeg = crystalHeight / (crystalHeight - lightHeight);
        return 2.0 * tiltSigma * Math.sin(elevation) / (1.0 + eyeOverLightLeg);
    }

    /**
     * The radiance of the pillar toward a direction, per unit intensity of the light and per unit face area of the
     * plates per volume, with that face area the same at every height.
     * The lateral angle is between the direction and the vertical plane through the eye and the light.
     * The pillar rises from the light, and the result is 0 where no crystal above both the eye and the light can
     * mirror it.
     * The tilt of the face that mirrors the light changes along the view ray, so the radiance integrates the reflected
     * light over that tilt with the 8 point Gauss Legendre rule. Crystals behind the eye are left out, which dims the
     * pillar where its crystals come close to the eye.
     */
    static double radiance(double elevation, double lateralAngle, double horizontalDistance, double lightHeight,
            double tiltSigma, double refractiveIndex) {
        double lightElevation = Math.atan2(lightHeight, horizontalDistance);
        double sineOfOffset = Math.sin(elevation - lightElevation);
        if (!(sineOfOffset > 0.0)) {
            return 0.0;
        }
        double lightToRay = Math.hypot(horizontalDistance, lightHeight) * sineOfOffset;
        if (!(lightToRay > 0.0)) {
            return 0.0;
        }
        double lowestTilt = lowestTilt(elevation, tiltSigma);
        double highestTilt = highestTilt(elevation, lightElevation, tiltSigma);
        if (!(highestTilt > lowestTilt)) {
            return 0.0;
        }
        double middleTilt = 0.5 * (highestTilt + lowestTilt);
        double halfRange = 0.5 * (highestTilt - lowestTilt);
        double sineOfElevation = Math.sin(elevation);
        double sum = 0.0;
        for (int node = 0; node < NODES.length; node++) {
            double alongTilt = middleTilt + halfRange * NODES[node];
            double lightDepression = elevation - 2.0 * alongTilt;
            double eyeOverLightLeg = Math.sin(lightDepression + lightElevation) / sineOfOffset;
            double acrossTilt = lateralAngle * (1.0 + eyeOverLightLeg)
                    / (Math.sin(lightDepression) + sineOfElevation);
            double tilt = Math.sqrt(alongTilt * alongTilt + acrossTilt * acrossTilt);
            sum += WEIGHTS[node] * IceMirror.slabReflectance(Math.sin(elevation - alongTilt), refractiveIndex)
                    * IceMirror.tiltDensity(tilt, tiltSigma);
        }
        return sum * halfRange / (2.0 * lightToRay);
    }

    /**
     * The face area per volume at the height, as a share of the face area at the bottom of the diamond dust layer. It
     * is 1 below the bottom and falls with LightPillarSelection.DUST_SCALE_HEIGHT above it.
     */
    static double dustDensity(double height, double dustBottomHeight) {
        return Math.exp(-Math.max(0.0, height - dustBottomHeight) / LightPillarSelection.DUST_SCALE_HEIGHT);
    }

    /**
     * The optical depth of the dust along a straight path of the length between two heights, for the extinction per
     * block at the bottom of the layer.
     */
    static double dustOpticalDepth(double fromHeight, double toHeight, double length, double extinction,
            double dustBottomHeight) {
        double rise = toHeight - fromHeight;
        double meanDensity = Math.abs(rise) > SMALLEST_RISE
                ? (dustColumn(toHeight, dustBottomHeight) - dustColumn(fromHeight, dustBottomHeight)) / rise
                : dustDensity(0.5 * (fromHeight + toHeight), dustBottomHeight);
        return extinction * length * meanDensity;
    }

    /**
     * The integral of dustDensity over height from the bottom of the layer up to the height.
     */
    private static double dustColumn(double height, double dustBottomHeight) {
        double above = height - dustBottomHeight;
        if (above <= 0.0) {
            return above;
        }
        return LightPillarSelection.DUST_SCALE_HEIGHT
                * (1.0 - Math.exp(-above / LightPillarSelection.DUST_SCALE_HEIGHT));
    }

    /**
     * The radiance of the pillar in the diamond dust, as dustRadiance gives it with every crystal counted near the eye
     * and with no blocks above the ground around the light.
     */
    static double dustRadiance(double elevation, double lateralAngle, double horizontalDistance, double lightHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double dustBottomHeight, double extinction,
            double sceneDistance, double horizon) {
        return dustRadiance(elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, refractiveIndex,
                lateralBlur, dustBottomHeight, extinction, sceneDistance, 0.0, horizon, horizon, horizon);
    }

    /**
     * The radiance of the pillar in the diamond dust, as radiance gives it but per unit face area of the plates per
     * volume at the bottom of the dust. Each reflection takes the face area at its crystals from dustDensity and is
     * dimmed along its way from the light and to the eye by the extinction per block at the bottom of the dust.
     * The lateral blur in radians widens the pillar on the sky without changing its light. Only crystals nearer to the
     * eye than the scene distance count, which is infinite where no scene stands. Crystals nearer to the eye than the
     * near fade in blocks count less, from none at NEAR_FADE_START_SHARE of it, unless it is 0. Only crystals whose light
     * leaves the light rising between the horizon and the ceiling or above the top of LightPillarHorizons count, in
     * radians. light_pillars.glsl computes the same.
     */
    static double dustRadiance(double elevation, double lateralAngle, double horizontalDistance, double lightHeight,
            double tiltSigma, double refractiveIndex, double lateralBlur, double dustBottomHeight, double extinction,
            double sceneDistance, double nearFade, double horizon, double ceiling, double top) {
        return dustIntegral(false, elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, refractiveIndex,
                lateralBlur, dustBottomHeight, extinction, sceneDistance, nearFade, horizon, ceiling, top);
    }

    /**
     * The mean count of crystals that mirror the light toward a direction, per unit face area of the plates per volume
     * at the bottom of the dust and per unit crystalCountScale, among the crystals that dustRadiance with the same
     * arguments counts. A face mirrors the light while it lies within the spread of its reflection by diffraction,
     * which widens as the face is seen more nearly edge on.
     */
    static double dustCrystalCount(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double tiltSigma, double lateralBlur, double dustBottomHeight, double sceneDistance,
            double nearFade, double horizon, double ceiling, double top) {
        return dustIntegral(true, elevation, lateralAngle, horizontalDistance, lightHeight, tiltSigma, 1.0,
                lateralBlur, dustBottomHeight, 0.0, sceneDistance, nearFade, horizon, ceiling, top);
    }

    /**
     * The share of the light that dustRadiance gives without a scene that comes from the crystals nearer to the eye
     * than the scene distance, with each tilt weighed by its Gaussian and by the near weight alone instead of the whole
     * integrand, and the near weight taken to fall linearly in the tilt. It is 1 where the scene distance is infinite.
     * light_pillars.glsl keeps this share of the value of a cell at each of its pixels, over the tilts within its own
     * range.
     */
    static double sceneShare(double elevation, double horizontalDistance, double lightHeight, double tiltSigma,
            double sceneDistance, double nearFade, double horizon, double ceiling, double top) {
        if (!(sceneDistance < Double.POSITIVE_INFINITY)) {
            return 1.0;
        }
        double lightElevation = Math.atan2(lightHeight, horizontalDistance);
        double sineOfOffset = Math.sin(elevation - lightElevation);
        if (!(sineOfOffset > 0.0)) {
            return 0.0;
        }
        double lightDistance = Math.hypot(horizontalDistance, lightHeight);
        double lightToRay = lightDistance * sineOfOffset;
        double lowestTilt = lowestTilt(elevation, tiltSigma);
        double cutTilt = tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay, sceneDistance);
        if (!(cutTilt > lowestTilt)) {
            return 1.0;
        }
        double highestTilt = highestTilt(elevation, lightElevation, tiltSigma, lightDistance, lightToRay, nearFade);
        double fullTilt = highestTilt;
        double startTilt = highestTilt;
        if (nearFade > 0.0) {
            fullTilt = tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay, nearFade);
            startTilt = tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay,
                    NEAR_FADE_START_SHARE * nearFade);
        }
        double aboveTop = Math.min(highestTilt, 0.5 * (elevation - top));
        double belowCeiling = Math.max(lowestTilt, 0.5 * (elevation - ceiling));
        double aboveHorizon = Math.min(highestTilt, 0.5 * (elevation - horizon));
        double whole = nearTiltMass(lowestTilt, aboveTop, fullTilt, startTilt, tiltSigma)
                + nearTiltMass(belowCeiling, aboveHorizon, fullTilt, startTilt, tiltSigma);
        if (!(whole > 0.0)) {
            return 0.0;
        }
        double nearer = nearTiltMass(cutTilt, aboveTop, fullTilt, startTilt, tiltSigma)
                + nearTiltMass(Math.max(belowCeiling, cutTilt), aboveHorizon, fullTilt, startTilt, tiltSigma);
        return Math.max(0.0, Math.min(1.0, nearer / whole));
    }

    /**
     * The mass of the tilt Gaussian from the lowest to the highest tilt, weighed by the near weight, which falls from 1
     * at the full tilt to 0 at the start tilt, here linearly in the tilt. The full and start tilts are those of the
     * crystals at the near fade and at its start.
     */
    private static double nearTiltMass(double lowestTilt, double highestTilt, double fullTilt, double startTilt,
            double tiltSigma) {
        double mass = tiltMass(lowestTilt, Math.min(highestTilt, fullTilt), tiltSigma);
        double rampLowest = Math.max(lowestTilt, fullTilt);
        double rampHighest = Math.min(highestTilt, startTilt);
        if (rampHighest > rampLowest) {
            // The integral of the start tilt less the tilt, times the Gaussian:
            double rampMass = tiltMass(rampLowest, rampHighest, tiltSigma);
            double weighed = startTilt * rampMass + tiltSigma * (normalDensity(rampHighest / tiltSigma)
                    - normalDensity(rampLowest / tiltSigma));
            mass += Math.max(0.0, Math.min(rampMass, weighed / (startTilt - fullTilt)));
        }
        return mass;
    }

    private static double normalDensity(double deviation) {
        return Math.exp(-0.5 * deviation * deviation) / Math.sqrt(2.0 * Math.PI);
    }

    /**
     * The standard normal distribution function from a logistic curve, within about 2e-4.
     */
    static double normalShare(double deviation) {
        return 1.0 / (1.0 + Math.exp(-1.5976 * deviation * (1.0 + 0.04417 * deviation * deviation)));
    }

    private static double tiltMass(double lowestTilt, double highestTilt, double tiltSigma) {
        if (!(highestTilt > lowestTilt)) {
            return 0.0;
        }
        return normalShare(highestTilt / tiltSigma) - normalShare(lowestTilt / tiltSigma);
    }

    private static double dustIntegral(boolean countsCrystals, double elevation, double lateralAngle,
            double horizontalDistance, double lightHeight, double tiltSigma, double refractiveIndex,
            double lateralBlur, double dustBottomHeight, double extinction, double sceneDistance, double nearFade,
            double horizon, double ceiling, double top) {
        double lightElevation = Math.atan2(lightHeight, horizontalDistance);
        double sineOfOffset = Math.sin(elevation - lightElevation);
        if (!(sineOfOffset > 0.0)) {
            return 0.0;
        }
        double lightDistance = Math.hypot(horizontalDistance, lightHeight);
        double lightToRay = lightDistance * sineOfOffset;
        if (!(lightToRay > 0.0)) {
            return 0.0;
        }
        double lowestTilt = Math.max(lowestTilt(elevation, tiltSigma),
                tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay, sceneDistance));
        double highestTilt = highestTilt(elevation, lightElevation, tiltSigma, lightDistance, lightToRay, nearFade);
        // The light leaves the light rising at the elevation minus twice the tilt:
        double sum = tiltSum(countsCrystals, lowestTilt, Math.min(highestTilt, 0.5 * (elevation - top)), elevation,
                lateralAngle, lightHeight, lightElevation, lightDistance, lightToRay, tiltSigma, refractiveIndex,
                lateralBlur, dustBottomHeight, extinction, nearFade)
                + tiltSum(countsCrystals, Math.max(lowestTilt, 0.5 * (elevation - ceiling)),
                Math.min(highestTilt, 0.5 * (elevation - horizon)), elevation, lateralAngle, lightHeight,
                lightElevation, lightDistance, lightToRay, tiltSigma, refractiveIndex, lateralBlur, dustBottomHeight,
                extinction, nearFade);
        if (countsCrystals) {
            return 2.0 * sum / lightToRay;
        }
        return sum / (2.0 * lightToRay);
    }

    /**
     * The reflections of dustRadiance, or the crystals of dustCrystalCount, for the tilts from the lowest to the
     * highest tilt, summed with the 8 point Gauss Legendre rule.
     */
    private static double tiltSum(boolean countsCrystals, double lowestTilt, double highestTilt, double elevation,
            double lateralAngle, double lightHeight, double lightElevation, double lightDistance, double lightToRay,
            double tiltSigma, double refractiveIndex, double lateralBlur, double dustBottomHeight, double extinction,
            double nearFade) {
        if (!(highestTilt > lowestTilt)) {
            return 0.0;
        }
        double middleTilt = 0.5 * (highestTilt + lowestTilt);
        double halfRange = 0.5 * (highestTilt - lowestTilt);
        double sineOfElevation = Math.sin(elevation);
        double tiltVariance = tiltSigma * tiltSigma;
        double sum = 0.0;
        for (int node = 0; node < NODES.length; node++) {
            double alongTilt = middleTilt + halfRange * NODES[node];
            double lightDepression = elevation - 2.0 * alongTilt;
            double sineAtCrystal = Math.max(Math.sin(elevation + lightDepression), SMALLEST_SINE_AT_CRYSTAL);
            double eyeLeg = lightDistance * Math.sin(lightDepression + lightElevation) / sineAtCrystal;
            double lightLeg = lightToRay / sineAtCrystal;
            double crystalHeight = eyeLeg * sineOfElevation;
            double acrossScale = (1.0 + eyeLeg / lightLeg) / (Math.sin(lightDepression) + sineOfElevation);
            double acrossVariance = tiltVariance + lateralBlur * lateralBlur * acrossScale * acrossScale;
            if (countsCrystals) {
                // The crystals in the pixel's cone along the ray, whose faces lie within the spread of the reflection:
                double sineOfGrazing = Math.max(Math.sin(elevation - alongTilt), SMALLEST_SINE_OF_GRAZING);
                sum += WEIGHTS[node] * gaussian(alongTilt, tiltVariance)
                        * gaussian(lateralAngle * acrossScale, acrossVariance)
                        * dustDensity(crystalHeight, dustBottomHeight) * nearWeight(eyeLeg, nearFade)
                        * eyeLeg * eyeLeg * lightLeg * lightLeg / (sineOfGrazing * sineOfGrazing);
            } else {
                double opticalDepth = dustOpticalDepth(lightHeight, crystalHeight, lightLeg, extinction,
                        dustBottomHeight) + dustOpticalDepth(crystalHeight, 0.0, eyeLeg, extinction, dustBottomHeight);
                sum += WEIGHTS[node] * IceMirror.slabReflectance(Math.sin(elevation - alongTilt), refractiveIndex)
                        * gaussian(alongTilt, tiltVariance) * gaussian(lateralAngle * acrossScale, acrossVariance)
                        * dustDensity(crystalHeight, dustBottomHeight) * Math.exp(-opticalDepth)
                        * nearWeight(eyeLeg, nearFade);
            }
        }
        return sum * halfRange;
    }

    /**
     * The radiance of the pillar of level faces in the diamond dust toward a direction, the closed form that
     * light_pillars.glsl draws, per unit face area of the plates per volume at the bottom of the dust as dustRadiance
     * gives it. Its crystals stand on the vertical sheet through the light at the elevation. It shows from the
     * elevation where crystals stand above both the eye and the light, fading in there over the fade in sigmas of the
     * tilt from the base share up, and crystals nearer to the eye than the near fade in blocks show less, as
     * nearWeight gives it. The rim of LightPillarShelter, an angle, lifts that elevation, and the pillar shows below
     * the cap, a height relative to the eye. A light as wide as twice the half width across the view in blocks spreads
     * its pillar over that width, as lateralProfile gives it.
     */
    static double closedFormRadiance(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double halfWidthAcross, double tiltSigma, double refractiveIndex, double lateralBlur,
            double dustBottomHeight, double extinction, double fadeInSigmas, double baseShare, double nearFade,
            double rim, double cap) {
        return closedForm(false, elevation, lateralAngle, horizontalDistance, lightHeight, halfWidthAcross, tiltSigma,
                refractiveIndex, lateralBlur, dustBottomHeight, extinction, fadeInSigmas, baseShare, nearFade, rim,
                cap);
    }

    /**
     * The mean count of crystals that mirror one light toward a direction in the closed form of closedFormRadiance,
     * per unit face area of the plates per volume at the bottom of the dust and per unit crystalCountScale, as
     * dustCrystalCount gives it. The fade in, the near fade, and the cap dim the light of the crystals but leave their
     * count, so glints there dim instead of thinning out. Each light of a cluster has crystals of its own, so the
     * cluster's count is this times its count of lights.
     */
    static double closedFormCrystalCount(double elevation, double lateralAngle, double horizontalDistance,
            double lightHeight, double halfWidthAcross, double tiltSigma, double lateralBlur, double dustBottomHeight,
            double fadeInSigmas, double baseShare, double nearFade, double rim, double cap) {
        return closedForm(true, elevation, lateralAngle, horizontalDistance, lightHeight, halfWidthAcross, tiltSigma,
                1.0, lateralBlur, dustBottomHeight, 0.0, fadeInSigmas, baseShare, nearFade, rim, cap);
    }

    /**
     * The share of the pillar's light per radian across it at the lateral angle, for the spread across the pillar of
     * one point of the light and the half angle that the light's width takes up across the view. It is the Gaussian
     * of the spread smeared evenly over the half angle on both sides, and integrates to 1 across the pillar. Where the
     * half angle is below SMEAR_SHARE of the spread, it is the Gaussian with the same variance instead, which is the
     * plain Gaussian for a half angle of 0.
     */
    static double lateralProfile(double lateralAngle, double spread, double halfAngle) {
        if (halfAngle <= SMEAR_SHARE * spread) {
            return gaussian(lateralAngle, spread * spread + halfAngle * halfAngle / 3.0);
        }
        double offset = Math.abs(lateralAngle);
        return (upperNormalTail((offset - halfAngle) / spread) - upperNormalTail((offset + halfAngle) / spread))
                / (2.0 * halfAngle);
    }

    /**
     * Half the width across the view of a cluster with the half extents along x and z, seen in the level direction of
     * the offset from the eye, or its widest half extent where the offset is 0.
     */
    static double halfWidthAcross(double halfExtentX, double halfExtentZ, double offsetX, double offsetZ) {
        double distance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (!(distance > 0.0)) {
            return Math.max(halfExtentX, halfExtentZ);
        }
        return (Math.abs(halfExtentX * offsetZ) + Math.abs(halfExtentZ * offsetX)) / distance;
    }

    /**
     * The chance that a standard normal deviate lies above the deviation, from the complementary error function of
     * Abramowitz and Stegun 7.1.26, within 1e-7.
     */
    static double upperNormalTail(double deviation) {
        double scaled = Math.abs(deviation) / Math.sqrt(2.0);
        double step = 1.0 / (1.0 + ERROR_FUNCTION_STEP * scaled);
        double polynomial = 0.0;
        for (int power = ERROR_FUNCTION_COEFFICIENTS.length - 1; power >= 0; power--) {
            polynomial = (polynomial + ERROR_FUNCTION_COEFFICIENTS[power]) * step;
        }
        double tail = 0.5 * polynomial * Math.exp(-scaled * scaled);
        return deviation >= 0.0 ? tail : 1.0 - tail;
    }

    /**
     * The lowest elevation of the closed form, where crystals stand above both the eye and the light, and not below
     * the rim's angle.
     */
    static double lowestClosedFormElevation(double horizontalDistance, double lightHeight, double rim) {
        return Math.max(Math.abs(Math.atan2(lightHeight, horizontalDistance)), rim);
    }

    /**
     * The share of its light that the closed form shows at the elevation above its lowest elevation, rising from 0
     * over the fade in sigmas of the tilt, and from the base share to all of it on top of that.
     */
    static double fadeIn(double elevation, double lowestElevation, double tiltSigma, double fadeInSigmas,
            double baseShare) {
        double ramp = smoothstep(lowestElevation, lowestElevation + fadeInSigmas * tiltSigma, elevation);
        return ramp * (baseShare + (1.0 - baseShare) * ramp);
    }

    /**
     * The share of its light that the closed form shows at the height of the sheet through the light above the eye,
     * falling over SHELTER_BAND below the cap, or all of it for an infinite cap.
     */
    static double capShare(double sheetHeight, double cap) {
        if (cap == Double.POSITIVE_INFINITY) {
            return 1.0;
        }
        return 1.0 - smoothstep(cap - SHELTER_BAND, cap, sheetHeight);
    }

    /**
     * The cap that the closed form takes relative to the eye, on the sheet through a light at the horizontal distance
     * from the eye, from its cap as a world height and its ceiling toward the eye, at most OPEN_CAP_HEIGHT. Light that
     * left the light at an elevation shows at the same elevation from the eye, so a ceiling cuts the sheet at the
     * distance times its tangent, and the lower of that and the cap counts. LightPillarShelter.OPEN_CEILING cuts
     * nothing.
     */
    static double shelteredCap(double cap, double eyeY, double horizontalDistance, double ceiling) {
        double capAboveEye = cap - eyeY;
        if (ceiling < LightPillarShelter.OPEN_CEILING) {
            capAboveEye = Math.min(capAboveEye, horizontalDistance * Math.tan(ceiling));
        }
        return Math.min(OPEN_CAP_HEIGHT, capAboveEye);
    }

    /**
     * The distance that vanilla's fog takes for a point at the offset from the eye, as fog_distance in vanilla's
     * fog.glsl. A cylinder takes the larger of the horizontal and the vertical distance, and a sphere the distance.
     */
    static double fogDistance(double offsetX, double offsetY, double offsetZ, boolean cylinder) {
        double horizontalSquared = offsetX * offsetX + offsetZ * offsetZ;
        if (cylinder) {
            return Math.max(Math.sqrt(horizontalSquared), Math.abs(offsetY));
        }
        return Math.sqrt(horizontalSquared + offsetY * offsetY);
    }

    /**
     * The share of a light that shows through vanilla's fog at the fog distance, as linear_fog in vanilla's fog.glsl
     * with the fog color's opacity. All of it shows up to the fog start, and the fog takes it over to the fog end.
     */
    static double fogVisibility(double fogDistance, double fogStart, double fogEnd, double fogOpacity) {
        if (fogDistance <= fogStart) {
            return 1.0;
        }
        double fogValue = fogDistance < fogEnd ? smoothstep(fogStart, fogEnd, fogDistance) : 1.0;
        return 1.0 - fogValue * Math.max(0.0, Math.min(1.0, fogOpacity));
    }

    /**
     * The smoothstep of GLSL, which steps at the edge where both edges are the same.
     */
    private static double smoothstep(double edge, double otherEdge, double value) {
        if (!(otherEdge > edge)) {
            return value >= edge ? 1.0 : 0.0;
        }
        double share = Math.max(0.0, Math.min(1.0, (value - edge) / (otherEdge - edge)));
        return share * share * (3.0 - 2.0 * share);
    }

    private static double closedForm(boolean countsCrystals, double elevation, double lateralAngle,
            double horizontalDistance, double lightHeight, double halfWidthAcross, double tiltSigma,
            double refractiveIndex, double lateralBlur, double dustBottomHeight, double extinction,
            double fadeInSigmas, double baseShare, double nearFade, double rim, double cap) {
        double lowestElevation = lowestClosedFormElevation(horizontalDistance, lightHeight, rim);
        if (!(elevation > lowestElevation)) {
            return 0.0;
        }
        double sineOfElevation = Math.sin(elevation);
        double sheetHeight = horizontalDistance * Math.tan(elevation);
        double crystalHeight = mirrorHeight(elevation, horizontalDistance, lightHeight);
        double eyeLeg = crystalHeight / sineOfElevation;
        double lightLeg = (crystalHeight - lightHeight) / sineOfElevation;
        double tiltSpread = 2.0 * tiltSigma * sineOfElevation * lightLeg / (lightLeg + eyeLeg);
        double spread = Math.sqrt(tiltSpread * tiltSpread + lateralBlur * lateralBlur);
        double across = lateralProfile(lateralAngle, spread, halfWidthAcross / horizontalDistance);
        double crystalsOverDistance = dustDensity(crystalHeight, dustBottomHeight) * across / horizontalDistance;
        if (countsCrystals) {
            double legsOverSpread = eyeLeg * lightLeg / Math.max(sineOfElevation, SMALLEST_SINE_OF_GRAZING);
            return 2.0 * crystalsOverDistance * legsOverSpread * legsOverSpread;
        }
        double weight = fadeIn(elevation, lowestElevation, tiltSigma, fadeInSigmas, baseShare)
                * capShare(sheetHeight, cap) * nearWeight(eyeLeg, nearFade);
        if (!(weight > 0.0)) {
            return 0.0;
        }
        double opticalDepth = extinction * (2.0 * dustColumn(crystalHeight, dustBottomHeight)
                - dustColumn(lightHeight, dustBottomHeight) - dustColumn(0.0, dustBottomHeight)) / sineOfElevation;
        return 0.5 * weight * crystalsOverDistance * Math.exp(-opticalDepth)
                * IceMirror.slabReflectance(sineOfElevation, refractiveIndex);
    }

    /**
     * Turns dustCrystalCount times the face area per volume in 1 per block into the mean count of crystals in a sky
     * cell one pixel wide, for the angle of a pixel and the diameter of the plates in micrometers, or 0 where the
     * diameter is 0. A face of area A reflects into a solid angle of about lambda^2 / A, so its face directions within
     * lambda^2 / (4 A) mirror the light, and each unit of face area per volume is 1 / A crystals.
     */
    static double crystalCountScale(double pixelAngle, double crystalMicrometers) {
        if (!(crystalMicrometers > 0.0)) {
            return 0.0;
        }
        double diameter = crystalMicrometers * 1.0e-6;
        double faceArea = 3.0 * Math.sqrt(3.0) / 8.0 * diameter * diameter;
        return pixelAngle * pixelAngle * WAVELENGTH * WAVELENGTH / (4.0 * faceArea * faceArea);
    }

    /**
     * The most that the crystals of a sky cell show, as a multiple of their mean value, for their mean count. It
     * covers counts up to three standard deviations and one crystal above the mean.
     */
    static double sparkleCap(double meanCount) {
        if (!(meanCount > 0.0)) {
            return MOST_SPARKLE;
        }
        return Math.min(MOST_SPARKLE, 1.0 + 3.0 / Math.sqrt(meanCount) + 1.0 / meanCount);
    }

    /**
     * The share of its mean value that a pillar shows in a sky cell, as light_pillars.glsl draws it, for the mean count
     * of crystals in the cell, the cell's place along the lateral angle and the elevation in pixels or on the grid of
     * LightPillarCells, the ribbon, and the twinkle clock in twinkle periods. Each cell draws a count of crystals per
     * period and blends to the next.
     */
    static double sparkle(double meanCount, int lateralCell, int elevationCell, int ribbon, double twinkleClock) {
        if (!(meanCount > 0.0)) {
            return 1.0;
        }
        int cellSeed = hash(lateralCell ^ hash(elevationCell ^ hash(ribbon)));
        double time = twinkleClock + uniform(cellSeed);
        double frame = Math.floor(time);
        int frameNumber = (int) frame;
        double cap = sparkleCap(meanCount);
        double before = Math.min(crystalsDrawn(meanCount, hash(cellSeed + frameNumber * GOLDEN_RATIO_STEP))
                / meanCount, cap);
        double after = Math.min(crystalsDrawn(meanCount, hash(cellSeed + (frameNumber + 1) * GOLDEN_RATIO_STEP))
                / meanCount, cap);
        double blend = Math.max(0.0, Math.min(1.0, 2.0 * (time - frame) - 0.5));
        return before + (after - before) * blend * blend * (3.0 - 2.0 * blend);
    }

    /**
     * A count of crystals for their mean count, from the Poisson distribution up to LARGEST_COUNTED_MEAN and above from
     * a normal deviate by the Cornish Fisher expansion of the Poisson distribution's quantile, rounded to a whole count.
     */
    static double crystalsDrawn(double meanCount, int seed) {
        double chance = uniform(seed);
        if (meanCount > LARGEST_COUNTED_MEAN) {
            double angle = 2.0 * Math.PI * uniform(hash(seed));
            double deviate = Math.sqrt(-2.0 * Math.log(Math.max(chance, 1.0e-7))) * Math.cos(angle);
            double root = Math.sqrt(meanCount);
            double quantile = meanCount + root * deviate + (deviate * deviate - 1.0) / 6.0
                    + deviate * (1.0 - deviate * deviate) / (72.0 * root);
            return Math.max(0.0, Math.floor(quantile + 0.5));
        }
        double term = Math.exp(-meanCount);
        double cumulative = term;
        int count = 0;
        while (chance > cumulative && count < MOST_DRAWN) {
            count++;
            term *= meanCount / count;
            cumulative += term;
        }
        return count;
    }

    /**
     * The lowbias32 integer hash by Chris Wellons.
     */
    private static int hash(int value) {
        int mixed = value;
        mixed ^= mixed >>> 16;
        mixed *= 0x7feb352d;
        mixed ^= mixed >>> 15;
        mixed *= 0x846ca68b;
        mixed ^= mixed >>> 16;
        return mixed;
    }

    /**
     * A number from 0 up to 1 from the highest 24 bits of a hash.
     */
    private static double uniform(int hashed) {
        return (hashed >>> 8) / 16777216.0;
    }

    /**
     * The share that crystals at the distance from the eye count with for the near fade, from none at
     * NEAR_FADE_START_SHARE of it up to all at the near fade, or all when the near fade is 0.
     */
    static double nearWeight(double eyeLeg, double nearFade) {
        if (!(nearFade > 0.0)) {
            return 1.0;
        }
        double start = NEAR_FADE_START_SHARE * nearFade;
        double share = Math.max(0.0, Math.min(1.0, (eyeLeg - start) / (nearFade - start)));
        return share * share * (3.0 - 2.0 * share);
    }

    /**
     * The tilt of the crystals on the view ray at the distance from the eye. The distance falls as the tilt rises.
     */
    private static double tiltAtDistance(double elevation, double lightElevation, double lightDistance,
            double lightToRay, double distance) {
        return elevation - 0.5 * Math.atan2(lightToRay, lightDistance * Math.cos(elevation - lightElevation) - distance);
    }

    /**
     * The largest standard deviation across the pillar at the elevation, in radians on the sky, among the reflections
     * that dustRadiance sums without a scene for the horizon. The pillar falls off sideways at least as fast as a
     * Gaussian of this width.
     */
    static double widestLateralSpread(double elevation, double horizontalDistance, double lightHeight,
            double tiltSigma, double lateralBlur, double horizon) {
        return widestLateralSpread(elevation, horizontalDistance, lightHeight, tiltSigma, lateralBlur, 0.0, horizon,
                horizon, horizon);
    }

    /**
     * The largest standard deviation across the pillar at the elevation, in radians on the sky, among the reflections
     * that dustRadiance sums without a scene for the near fade, the horizon, the ceiling, and the top.
     */
    static double widestLateralSpread(double elevation, double horizontalDistance, double lightHeight,
            double tiltSigma, double lateralBlur, double nearFade, double horizon, double ceiling, double top) {
        double lightElevation = Math.atan2(lightHeight, horizontalDistance);
        double sineOfOffset = Math.sin(elevation - lightElevation);
        if (!(sineOfOffset > 0.0)) {
            return 0.0;
        }
        double lightDistance = Math.hypot(horizontalDistance, lightHeight);
        double lightToRay = lightDistance * sineOfOffset;
        double lowestTilt = lowestTilt(elevation, tiltSigma);
        double highestTilt = highestTilt(elevation, lightElevation, tiltSigma, lightDistance, lightToRay, nearFade);
        return Math.max(
                spreadOver(lowestTilt, Math.min(highestTilt, 0.5 * (elevation - top)), elevation, lightElevation,
                        sineOfOffset, tiltSigma, lateralBlur),
                spreadOver(Math.max(lowestTilt, 0.5 * (elevation - ceiling)),
                        Math.min(highestTilt, 0.5 * (elevation - horizon)), elevation, lightElevation, sineOfOffset,
                        tiltSigma, lateralBlur));
    }

    private static double spreadOver(double lowestTilt, double highestTilt, double elevation, double lightElevation,
            double sineOfOffset, double tiltSigma, double lateralBlur) {
        if (!(highestTilt > lowestTilt)) {
            return 0.0;
        }
        double middleTilt = 0.5 * (highestTilt + lowestTilt);
        double halfRange = 0.5 * (highestTilt - lowestTilt);
        double sineOfElevation = Math.sin(elevation);
        double widest = 0.0;
        for (double node : NODES) {
            double lightDepression = elevation - 2.0 * (middleTilt + halfRange * node);
            double eyeOverLightLeg = Math.sin(lightDepression + lightElevation) / sineOfOffset;
            double acrossScale = (1.0 + eyeOverLightLeg) / (Math.sin(lightDepression) + sineOfElevation);
            double spread = Math.sqrt(tiltSigma * tiltSigma / (acrossScale * acrossScale) + lateralBlur * lateralBlur);
            widest = Math.max(widest, spread);
        }
        return widest;
    }

    /**
     * The lowest elevation where the pillar can show. It is the light itself, or for a light below the eye, where
     * crystals above the eye mirror it with the most tilted faces that dustRadiance counts, or where those faces
     * mirror light that leaves the light at the horizon, whichever is highest.
     */
    static double lowestRibbonElevation(double horizontalDistance, double lightHeight, double tiltSigma,
            double horizon) {
        double lightElevation = Math.atan2(lightHeight, horizontalDistance);
        return Math.max(Math.max(lightElevation, -lightElevation - 2.0 * TILT_RANGE_SIGMAS * tiltSigma),
                horizon - 2.0 * TILT_RANGE_SIGMAS * tiltSigma);
    }

    /**
     * Half the width of the ribbon a pillar is drawn on at the elevation, the lateral angle beyond which the pillar
     * stays below the visible value, given its value at the center. It is 0 where the center is not visible.
     */
    static double ribbonHalfWidth(double centerValue, double visibleValue, double widestLateralSpread) {
        if (!(centerValue > visibleValue)) {
            return 0.0;
        }
        double spreads = Math.min(MOST_RIBBON_SPREADS, Math.sqrt(2.0 * Math.log(centerValue / visibleValue)));
        return spreads * widestLateralSpread;
    }

    /**
     * Half the width of the ribbon of a pillar whose crystals glitter, given its mean value and its mean count of
     * crystals per sky cell at the center. It holds the mean value down to the visible value as ribbonHalfWidth does
     * without glitter, and where the count is above 0 also the glints, so that beyond the half width a cell shows the
     * glint value with less than RIBBON_GLINT_CHANCE. Across the pillar the count falls with the value, so each
     * crystal shows about the same value.
     */
    static double ribbonHalfWidth(double centerValue, double meanCount, double visibleValue, double glintValue,
            double widestLateralSpread) {
        double smoothHalfWidth = ribbonHalfWidth(centerValue, visibleValue, widestLateralSpread);
        if (!(meanCount > 0.0)) {
            return smoothHalfWidth;
        }
        double leastCount = leastVisibleCount(centerValue / (meanCount * glintValue));
        if (!(meanCount > leastCount)) {
            return smoothHalfWidth;
        }
        double spreads = Math.min(MOST_RIBBON_SPREADS, Math.sqrt(2.0 * Math.log(meanCount / leastCount)));
        return Math.max(smoothHalfWidth, spreads * widestLateralSpread);
    }

    /**
     * The mean count of crystals per sky cell below which the cell shows the glint value with less than
     * RIBBON_GLINT_CHANCE, for the value of one crystal over the glint value, with RIBBON_GLINT_MARGIN. Where one or
     * two crystals show it, the chance is about the count to that power over its factorial, and from three on the
     * Wilson Hilferty approximation of the Poisson distribution gives it. sparkleCap and MOST_SPARKLE also keep cells
     * with few crystals below the glint value.
     */
    static double leastVisibleCount(double crystalOverGlint) {
        if (!(crystalOverGlint > 0.0)) {
            return Double.POSITIVE_INFINITY;
        }
        double ratio = 1.0 / ((1.0 + RIBBON_GLINT_MARGIN) * crystalOverGlint);
        double needed = Math.max(1.0, Math.ceil(ratio));
        double chanceCount;
        if (needed <= 2.0) {
            chanceCount = Math.pow(RIBBON_GLINT_CHANCE * needed, 1.0 / needed);
        } else {
            double root = Math.max(0.0, 1.0 - 1.0 / (9.0 * needed) - RIBBON_GLINT_SIGMAS / (3.0 * Math.sqrt(needed)));
            chanceCount = needed * root * root * root;
        }
        double capRoot = 0.5 * (Math.sqrt(4.0 * ratio + 5.0) - 3.0);
        double capCount = ratio > 1.0 ? capRoot * capRoot : 0.0;
        return Math.max(chanceCount, Math.max(capCount, ratio / MOST_SPARKLE));
    }

    /**
     * The lateral blur in radians that keeps a pillar narrower than a pixel from flickering, for the angle of a pixel
     * at the center of the screen.
     */
    static double lateralBlur(double pixelAngle) {
        return PIXEL_BLUR_FRACTION * pixelAngle;
    }

    private static double lowestTilt(double elevation, double tiltSigma) {
        return Math.max(-TILT_RANGE_SIGMAS * tiltSigma, elevation - 0.5 * Math.PI);
    }

    private static double highestTilt(double elevation, double lightElevation, double tiltSigma) {
        return Math.min(TILT_RANGE_SIGMAS * tiltSigma, 0.5 * (elevation + lightElevation));
    }

    /**
     * The highest tilt also cut where the crystals come nearer to the eye than the start of the near fade.
     */
    private static double highestTilt(double elevation, double lightElevation, double tiltSigma, double lightDistance,
            double lightToRay, double nearFade) {
        double highest = highestTilt(elevation, lightElevation, tiltSigma);
        if (!(nearFade > 0.0)) {
            return highest;
        }
        return Math.min(highest, tiltAtDistance(elevation, lightElevation, lightDistance, lightToRay,
                NEAR_FADE_START_SHARE * nearFade));
    }

    private static double gaussian(double value, double variance) {
        return Math.exp(-value * value / (2.0 * variance)) / Math.sqrt(2.0 * Math.PI * variance);
    }
}
