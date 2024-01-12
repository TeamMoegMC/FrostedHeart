/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.noise;


import static com.teammoeg.frostedheart.util.noise.NoiseUtil.*;

public class Cellular3D implements INoise3D {
    private final int seed;
    private final float jitter;
    private final CellularNoiseType returnType;

    private float centerX, centerY, centerZ;
    private float frequency;

    public Cellular3D(long seed) {
        this(seed, 1.0f, CellularNoiseType.VALUE);
    }

    public Cellular3D(long seed, float jitter, CellularNoiseType returnType) {
        this.seed = (int) seed;
        this.jitter = jitter;
        this.returnType = returnType;
        this.frequency = 1;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getCenterZ() {
        return centerZ;
    }

    @Override
    public float noise(float x, float y, float z) {
        x *= frequency;
        y *= frequency;
        z *= frequency;

        int xr = NoiseUtil.fastRound(x);
        int yr = NoiseUtil.fastRound(y);
        int zr = NoiseUtil.fastRound(z);

        float distance0 = Float.MAX_VALUE;
        float distance1 = Float.MAX_VALUE;
        int closestHash = 0;

        float cellularJitter = 0.39614353f * jitter;

        int xPrimed = (xr - 1) * PRIME_X;
        int yPrimedBase = (yr - 1) * PRIME_Y;
        int zPrimedBase = (zr - 1) * PRIME_Z;

        for (int xi = xr - 1; xi <= xr + 1; xi++) {
            int yPrimed = yPrimedBase;

            for (int yi = yr - 1; yi <= yr + 1; yi++) {
                int zPrimed = zPrimedBase;

                for (int zi = zr - 1; zi <= zr + 1; zi++) {
                    int hash = NoiseUtil.hashPrimed(seed, xPrimed, yPrimed, zPrimed);
                    int idx = hash & (255 << 2);

                    float vecX = (xi - x) + RANDOM_VECTORS_3D[idx] * cellularJitter;
                    float vecY = (yi - y) + RANDOM_VECTORS_3D[idx | 1] * cellularJitter;
                    float vecZ = (zi - z) + RANDOM_VECTORS_3D[idx | 2] * cellularJitter;

                    float newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ;

                    distance1 = NoiseUtil.fastMax(NoiseUtil.fastMin(distance1, newDistance), distance0);
                    if (newDistance < distance0) {
                        distance0 = newDistance;
                        closestHash = hash;
                        centerX = vecX + x;
                        centerY = vecY + y;
                        centerZ = vecZ + z;
                    }
                    zPrimed += PRIME_Z;
                }
                yPrimed += PRIME_Y;
            }
            xPrimed += PRIME_X;
        }

        centerX /= frequency;
        centerY /= frequency;
        centerZ /= frequency;

        return returnType.calculate(distance0, distance1, closestHash);
    }

    @Override
    public Cellular3D spread(float scaleFactor) {
        this.frequency *= scaleFactor;
        return this;
    }
}
