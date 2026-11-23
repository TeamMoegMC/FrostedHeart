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
 * Steps along a ray for the midpoint rule. Outside the ice band the steps are the cells of a uniform grid over the
 * whole ray, cut where the ray enters or leaves the band. Each crossing of the band gets its own equal steps, because
 * the thin ice layer would otherwise fall between one or two samples.
 */
final class SkyRaySteps {
    static final int MAXIMUM_BOUNDARIES = 6;
    /**
     * Steps per crossing of the ice band that the tables use.
     */
    static final int ICE_BAND_STEPS = 12;

    private SkyRaySteps() {
    }

    static int maximumSteps(int uniformSteps, int bandSteps) {
        return uniformSteps + 2 * bandSteps + MAXIMUM_BOUNDARIES;
    }

    /**
     * Writes the midpoint distance and length of each step from the offset on and returns the step count, without
     * allocating. The boundaries array holds at least MAXIMUM_BOUNDARIES values.
     */
    static int fill(double radius, double cosine, double length, int uniformSteps, int bandSteps,
            double[] boundaries, double[] travelled, double[] lengths, int offset) {
        int boundaryCount = 0;
        boundaries[boundaryCount++] = 0.0;
        boundaryCount = addCrossings(radius, cosine, length, SkyAtmosphere.ICE_BAND_LOW_RADIUS, boundaries,
                boundaryCount);
        boundaryCount = addCrossings(radius, cosine, length, SkyAtmosphere.ICE_BAND_HIGH_RADIUS, boundaries,
                boundaryCount);
        boundaries[boundaryCount++] = length;
        for (int index = 1; index < boundaryCount; index++) {
            double value = boundaries[index];
            int position = index - 1;
            while (position >= 0 && boundaries[position] > value) {
                boundaries[position + 1] = boundaries[position];
                position--;
            }
            boundaries[position + 1] = value;
        }
        double cellLength = length / uniformSteps;
        int count = 0;
        for (int segment = 0; segment + 1 < boundaryCount; segment++) {
            double start = boundaries[segment];
            double end = boundaries[segment + 1];
            if (end <= start) {
                continue;
            }
            double middleRadius = SkyAtmosphere.radiusAlongRay(radius, cosine, 0.5 * (start + end));
            if (middleRadius >= SkyAtmosphere.ICE_BAND_LOW_RADIUS && middleRadius <= SkyAtmosphere.ICE_BAND_HIGH_RADIUS) {
                double stepLength = (end - start) / bandSteps;
                for (int step = 0; step < bandSteps; step++) {
                    travelled[offset + count] = start + (step + 0.5) * stepLength;
                    lengths[offset + count] = stepLength;
                    count++;
                }
                continue;
            }
            for (int cell = Math.max(0, (int) (start / cellLength)); cell < uniformSteps; cell++) {
                double cellStart = Math.max(start, cell * cellLength);
                if (cellStart >= end) {
                    break;
                }
                double cellEnd = Math.min(end, (cell + 1) * cellLength);
                if (cellEnd > cellStart) {
                    travelled[offset + count] = 0.5 * (cellStart + cellEnd);
                    lengths[offset + count] = cellEnd - cellStart;
                    count++;
                }
            }
        }
        return count;
    }

    private static int addCrossings(double radius, double cosine, double length, double sphereRadius,
            double[] boundaries, int boundaryCount) {
        double discriminant = radius * radius * (cosine * cosine - 1.0) + sphereRadius * sphereRadius;
        if (discriminant <= 0.0) {
            return boundaryCount;
        }
        double root = Math.sqrt(discriminant);
        double near = -radius * cosine - root;
        double far = -radius * cosine + root;
        if (near > 0.0 && near < length) {
            boundaries[boundaryCount++] = near;
        }
        if (far > 0.0 && far < length) {
            boundaries[boundaryCount++] = far;
        }
        return boundaryCount;
    }
}
