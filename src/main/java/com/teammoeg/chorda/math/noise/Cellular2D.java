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
 * 二维细胞噪声（Worley噪声）实现，基于Voronoi图原理生成噪声。
 * 支持多种返回值类型和可配置的抖动参数。
 * <p>
 * 2D cellular noise (Worley noise) implementation based on Voronoi diagram principles.
 * Supports multiple return value types and configurable jitter parameters.
 */
public class Cellular2D implements INoise2D {
    private final int seed;
    private final float jitter;
    private final CellularNoiseType returnType;

    // Last computed values
    private float centerX, centerY;
    private int closestHash;
    private float distance0, distance1;

    // Modifiers
    private float frequency;

    /**
     * 使用默认参数构造二维细胞噪声。
     * <p>
     * Constructs a 2D cellular noise with default parameters.
     *
     * @param seed 随机种子 / the random seed
     */
    public Cellular2D(long seed) {
        this(seed, 1.0f, CellularNoiseType.VALUE);
    }

    /**
     * 使用指定参数构造二维细胞噪声。
     * <p>
     * Constructs a 2D cellular noise with specified parameters.
     *
     * @param seed 随机种子 / the random seed
     * @param jitter 特征点抖动量 / the feature point jitter amount
     * @param returnType 噪声返回值类型 / the noise return value type
     */
    public Cellular2D(long seed, float jitter, CellularNoiseType returnType) {
        this.seed = (int) seed;
        this.jitter = jitter;
        this.returnType = returnType;
        this.frequency = 1;
    }

    /**
     * 使用替代返回类型获取上次计算的噪声值。
     * <p>
     * Gets the last computed noise value using an alternate return type.
     *
     * @param alternateType 替代噪声类型 / the alternate noise type
     * @return 噪声值 / the noise value
     */
    public float get(CellularNoiseType alternateType) {
        return alternateType.calculate(distance0, distance1, closestHash);
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

    @Override
    public float noise(float x, float y) {
        x *= frequency;
        y *= frequency;

        int xr = NoiseUtil.fastRound(x);
        int yr = NoiseUtil.fastRound(y);

        distance0 = Float.MAX_VALUE;
        distance1 = Float.MAX_VALUE;
        closestHash = 0;

        float cellularJitter = 0.43701595f * jitter;

        int xPrimed = (xr - 1) * NoiseUtil.PRIME_X;
        int yPrimedBase = (yr - 1) * NoiseUtil.PRIME_Y;

        for (int xi = xr - 1; xi <= xr + 1; xi++) {
            int yPrimed = yPrimedBase;

            for (int yi = yr - 1; yi <= yr + 1; yi++) {
                int hash = NoiseUtil.hashPrimed(seed, xPrimed, yPrimed);
                int idx = hash & (255 << 1);

                float cellX = xi + NoiseUtil.RANDOM_VECTORS_2D[idx] * cellularJitter;
                float cellY = yi + NoiseUtil.RANDOM_VECTORS_2D[idx | 1] * cellularJitter;

                float vecX = (x - cellX);
                float vecY = (y - cellY);

                float newDistance = vecX * vecX + vecY * vecY;

                distance1 = NoiseUtil.fastMax(NoiseUtil.fastMin(distance1, newDistance), distance0);
                if (newDistance < distance0) {
                    distance0 = newDistance;
                    closestHash = hash;
                    centerX = cellX;
                    centerY = cellY;
                }
                yPrimed += NoiseUtil.PRIME_Y;
            }
            xPrimed += NoiseUtil.PRIME_X;
        }

        centerX /= frequency;
        centerY /= frequency;

        return returnType.calculate(distance0, distance1, closestHash);
    }

    @Override
    public Cellular2D spread(float scaleFactor) {
        this.frequency *= scaleFactor;
        return this;
    }
}
