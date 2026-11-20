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
 * Reflection off the two large faces of plate crystals, which float nearly level and act as small mirrors.
 * The mirrored sunlight forms a pillar through the sun's mirror image below the horizon. For a view in the sun's
 * azimuth at elevation e and a sun at elevation s, a face tilted by (e + s) / 2 reflects the sun into the view at the
 * grazing angle (e - s) / 2, so the pillar is the tilt spread doubled along the vertical and doubled times the sine of
 * the grazing angle across it.
 */
final class IceMirror {
    /**
     * The mean side area of a hexagonal prism seen from the side over its basal area, per unit of half height over
     * radius.
     */
    private static final double SIDE_OVER_BASAL_AREA = 8.0 / (Math.PI * Math.sqrt(3.0));
    private static final double SMALLEST_SIGMA = 1.0e-9;
    private static final double SHARE_SPAN_SIGMAS = 6.0;
    private static final int SHARE_STEPS = 480;

    private IceMirror() {
    }

    /**
     * The share of light a plate reflects, from its near face and from its far face through the plate, for
     * unpolarized light. Each polarization reflects 2 F / (1 + F) of the Fresnel reflectance F of one face.
     * The grazing angle is between the light's direction and the face.
     */
    static double slabReflectance(double sineOfGrazingAngle, double refractiveIndex) {
        double cosineOfIncidence = Math.max(0.0, Math.min(1.0, sineOfGrazingAngle));
        double squaredSineOfIncidence = 1.0 - cosineOfIncidence * cosineOfIncidence;
        double cosineOfRefraction = Math.sqrt(1.0 - squaredSineOfIncidence / (refractiveIndex * refractiveIndex));
        double perpendicular = (cosineOfIncidence - refractiveIndex * cosineOfRefraction)
                / (cosineOfIncidence + refractiveIndex * cosineOfRefraction);
        double parallel = (refractiveIndex * cosineOfIncidence - cosineOfRefraction)
                / (refractiveIndex * cosineOfIncidence + cosineOfRefraction);
        double perpendicularFace = perpendicular * perpendicular;
        double parallelFace = parallel * parallel;
        return perpendicularFace / (1.0 + perpendicularFace) + parallelFace / (1.0 + parallelFace);
    }

    /**
     * The density per steradian of face normals at the tilt from the vertical, for tilts spread as a Gaussian of the
     * standard deviation in radians. It integrates to 1 over small tilts.
     */
    static double tiltDensity(double tilt, double tiltSigma) {
        return Math.exp(-tilt * tilt / (2.0 * tiltSigma * tiltSigma)) / (2.0 * Math.PI * tiltSigma * tiltSigma);
    }

    /**
     * The mean area of a plate seen from the sun over its basal area, with its tilt spread as a Gaussian of the
     * standard deviation in radians.
     */
    static double crossSection(double sunElevation, double tiltSigma, double halfHeightOverRadius) {
        double height = Math.abs(Math.sin(sunElevation));
        double spread = Math.max(SMALLEST_SIGMA, tiltSigma * Math.cos(sunElevation));
        double ratio = height / spread;
        double basal = spread * Math.sqrt(2.0 / Math.PI) * Math.exp(-0.5 * ratio * ratio)
                + height * (1.0 - 2.0 * upperTail(ratio));
        return basal + SIDE_OVER_BASAL_AREA * halfHeightOverRadius * Math.cos(sunElevation);
    }

    /**
     * The standard deviation in radians of the pillar along the vertical, centered on the sun's mirror image.
     */
    static double pillarHeightSigma(double tiltSigma) {
        return Math.max(SMALLEST_SIGMA, 2.0 * tiltSigma);
    }

    /**
     * The standard deviation in radians of the pillar across it at a view elevation.
     */
    static double pillarWidthSigma(double viewElevation, double sunElevation, double tiltSigma) {
        return Math.max(SMALLEST_SIGMA, 2.0 * tiltSigma * Math.abs(Math.sin(0.5 * (viewElevation - sunElevation))));
    }

    /**
     * The share of the light hitting plates that the pillar holds per unit of a Gaussian of pillarHeightSigma along
     * the vertical and pillarWidthSigma across, at a view elevation. Over the whole pillar it adds up to pillarShare,
     * which for level plates is sin(s) R(s) over the cross section.
     */
    static double pillarStrength(double viewElevation, double sunElevation, double crossSection,
            double refractiveIndex) {
        double grazingSine = Math.abs(Math.sin(0.5 * (viewElevation - sunElevation)));
        return grazingSine * slabReflectance(grazingSine, refractiveIndex) / crossSection;
    }

    /**
     * The pillarStrength summed over the whole pillar, for the light hitting plates, which pillarPhase spreads over the
     * sky. It is close to the mirrored share but counts all light that enters an end face as leaving through it again.
     */
    static double pillarShare(double sunElevation, double tiltSigma, double crossSection, double refractiveIndex) {
        double heightSigma = pillarHeightSigma(tiltSigma);
        double lowest = Math.max(-0.5 * Math.PI, -sunElevation - SHARE_SPAN_SIGMAS * heightSigma);
        double highest = Math.min(0.5 * Math.PI, -sunElevation + SHARE_SPAN_SIGMAS * heightSigma);
        double step = (highest - lowest) / SHARE_STEPS;
        double sum = 0.0;
        for (int index = 0; index < SHARE_STEPS; index++) {
            double viewElevation = lowest + (index + 0.5) * step;
            sum += pillarStrength(viewElevation, sunElevation, crossSection, refractiveIndex)
                    * gaussian(viewElevation + sunElevation, heightSigma);
        }
        return sum * step;
    }

    /**
     * The pillar's phase per steradian toward a view elevation and an angle sideways from the vertical through the sun,
     * per unit of light hitting plates. The extra variance in squared radians widens it in both directions, as a blur.
     */
    static double pillarPhase(double viewElevation, double sideAngle, double sunElevation, double tiltSigma,
            double halfHeightOverRadius, double refractiveIndex, double extraVariance) {
        double heightSigma = pillarHeightSigma(tiltSigma);
        double widthSigma = pillarWidthSigma(viewElevation, sunElevation, tiltSigma);
        double crossSection = crossSection(sunElevation, tiltSigma, halfHeightOverRadius);
        return pillarStrength(viewElevation, sunElevation, crossSection, refractiveIndex)
                * gaussian(viewElevation + sunElevation, Math.sqrt(heightSigma * heightSigma + extraVariance))
                * gaussian(sideAngle, Math.sqrt(widthSigma * widthSigma + extraVariance));
    }

    /**
     * The probability of a standard normal variable between the two bounds.
     */
    static double gaussianMass(double lower, double upper) {
        if (lower >= 0.0) {
            return Math.max(0.0, upperTail(lower) - upperTail(upper));
        }
        if (upper <= 0.0) {
            return Math.max(0.0, upperTail(-upper) - upperTail(-lower));
        }
        return Math.max(0.0, 1.0 - upperTail(-lower) - upperTail(upper));
    }

    private static double gaussian(double offset, double sigma) {
        double ratio = offset / sigma;
        return Math.exp(-0.5 * ratio * ratio) / (Math.sqrt(2.0 * Math.PI) * sigma);
    }

    /**
     * The probability of a standard normal variable above the bound, from the complementary error function of
     * Numerical Recipes, with a relative error below 1.2e-7 everywhere.
     */
    private static double upperTail(double bound) {
        double argument = Math.abs(bound) / Math.sqrt(2.0);
        double t = 1.0 / (1.0 + 0.5 * argument);
        double complement = t * Math.exp(-argument * argument - 1.26551223 + t * (1.00002368 + t * (0.37409196
                + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587
                + t * (-0.82215223 + t * 0.17087277)))))))));
        return bound >= 0.0 ? 0.5 * complement : 1.0 - 0.5 * complement;
    }
}
