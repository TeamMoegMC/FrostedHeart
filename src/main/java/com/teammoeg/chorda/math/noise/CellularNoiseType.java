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
 * 细胞噪声（Worley噪声）的返回值类型枚举。注意：所有距离均为平方距离。
 * <p>
 * Return value type enumeration for cellular noise (Worley noise). Note: all distances are squared distances.
 */
public enum CellularNoiseType {
    VALUE,
    DISTANCE,
    DISTANCE_2,
    DISTANCE_SUM,
    DISTANCE_DIFFERENCE,
    DISTANCE_PRODUCT,
    DISTANCE_QUOTIENT,
    OTHER;

    /**
     * 根据噪声类型计算返回值。
     * <p>
     * Calculates the return value based on the noise type.
     *
     * @param distance0 到最近特征点的平方距离 / the squared distance to the closest feature point
     * @param distance1 到第二近特征点的平方距离 / the squared distance to the second closest feature point
     * @param closestHash 最近特征点的哈希值 / the hash of the closest feature point
     * @return 计算出的噪声值 / the computed noise value
     */
    public float calculate(float distance0, float distance1, int closestHash) {
        switch (this) {
            case VALUE:
                return closestHash * (1 / 2147483648.0f);
            case DISTANCE:
                return distance0;
            case DISTANCE_2:
                return distance1;
            case DISTANCE_SUM:
                return (distance1 + distance0) * 0.5f - 1;
            case DISTANCE_DIFFERENCE:
                return distance1 - distance0 - 1;
            case DISTANCE_PRODUCT:
                return distance1 * distance0 * 0.5f - 1;
            case DISTANCE_QUOTIENT:
                return distance0 / distance1 - 1;
            default:
                return 0;
        }
    }
}
