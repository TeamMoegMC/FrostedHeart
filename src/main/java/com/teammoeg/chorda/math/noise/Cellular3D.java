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

package com.teammoeg.chorda.math.noise;


/**
 * 三维细胞噪声（Worley噪声）实现，基于Voronoi图原理在三维空间中生成噪声。
 * <p>
 * 3D cellular noise (Worley noise) implementation based on Voronoi diagram principles in 3D space.
 */
public class Cellular3D implements INoise3D {
    private final int seed;
    private final float jitter;
    private final CellularNoiseType returnType;

    private float centerX, centerY, centerZ;
    private float frequency;

    /**
     * 使用默认参数构造三维细胞噪声。
     * <p>
     * Constructs a 3D cellular noise with default parameters.
     *
     * @param seed 随机种子 / the random seed
     */
    public Cellular3D(long seed) {
        this(seed, 1.0f, CellularNoiseType.VALUE);
    }

    /**
     * 使用指定参数构造三维细胞噪声。
     * <p>
     * Constructs a 3D cellular noise with specified parameters.
     *
     * @param seed 随机种子 / the random seed
     * @param jitter 特征点抖动量 / the feature point jitter amount
     * @param returnType 噪声返回值类型 / the noise return value type
     */
    public Cellular3D(long seed, float jitter, CellularNoiseType returnType) {
        this.seed = (int) seed;
        this.jitter = jitter;
        this.returnType = returnType;
        this.frequency = 1;
    }

    /**
     * 获取上次计算中最近特征点的X坐标。
     * <p>
     * Gets the X coordinate of the closest feature point from the last computation.
     *
     * @return 最近特征点的X坐标 / the X coordinate of the closest feature point
     */
    public float getCenterX() {
        return centerX;
    }

    /**
     * 获取上次计算中最近特征点的Y坐标。
     * <p>
     * Gets the Y coordinate of the closest feature point from the last computation.
     *
     * @return 最近特征点的Y坐标 / the Y coordinate of the closest feature point
     */
    public float getCenterY() {
        return centerY;
    }

    /**
     * 获取上次计算中最近特征点的Z坐标。
     * <p>
     * Gets the Z coordinate of the closest feature point from the last computation.
     *
     * @return 最近特征点的Z坐标 / the Z coordinate of the closest feature point
     */
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

        int xPrimed = (xr - 1) * NoiseUtil.PRIME_X;
        int yPrimedBase = (yr - 1) * NoiseUtil.PRIME_Y;
        int zPrimedBase = (zr - 1) * NoiseUtil.PRIME_Z;

        for (int xi = xr - 1; xi <= xr + 1; xi++) {
            int yPrimed = yPrimedBase;

            for (int yi = yr - 1; yi <= yr + 1; yi++) {
                int zPrimed = zPrimedBase;

                for (int zi = zr - 1; zi <= zr + 1; zi++) {
                    int hash = NoiseUtil.hashPrimed(seed, xPrimed, yPrimed, zPrimed);
                    int idx = hash & (255 << 2);

                    float vecX = (xi - x) + NoiseUtil.RANDOM_VECTORS_3D[idx] * cellularJitter;
                    float vecY = (yi - y) + NoiseUtil.RANDOM_VECTORS_3D[idx | 1] * cellularJitter;
                    float vecZ = (zi - z) + NoiseUtil.RANDOM_VECTORS_3D[idx | 2] * cellularJitter;

                    float newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ;

                    distance1 = NoiseUtil.fastMax(NoiseUtil.fastMin(distance1, newDistance), distance0);
                    if (newDistance < distance0) {
                        distance0 = newDistance;
                        closestHash = hash;
                        centerX = vecX + x;
                        centerY = vecY + y;
                        centerZ = vecZ + z;
                    }
                    zPrimed += NoiseUtil.PRIME_Z;
                }
                yPrimed += NoiseUtil.PRIME_Y;
            }
            xPrimed += NoiseUtil.PRIME_X;
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
