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
 * 一维噪声层的封装接口，支持扩展到二维、输入变换等操作。
 * <p>
 * Wrapper interface for a 1D noise layer, supporting extension to 2D, input transformation, and other operations.
 */
@FunctionalInterface
public interface INoise1D {
    /**
     * 创建一个三角波噪声函数。
     * <p>
     * Creates a triangle wave noise function.
     *
     * @param amplitude 振幅 / the amplitude
     * @param midpoint 中点值 / the midpoint value
     * @param frequency 频率 / the frequency
     * @param phaseShift 相位偏移 / the phase shift
     * @return 三角波噪声函数 / a triangle wave noise function
     */
    static INoise1D triangle(float amplitude, float midpoint, float frequency, float phaseShift) {
        return q -> triangle(amplitude, midpoint, frequency, phaseShift, q);
    }

    /**
     * 计算三角波噪声值。
     * <p>
     * Computes a triangle wave noise value.
     *
     * @param amplitude 振幅 / the amplitude
     * @param midpoint 中点值 / the midpoint value
     * @param frequency 频率 / the frequency
     * @param phaseShift 相位偏移 / the phase shift
     * @param q 输入值 / the input value
     * @return 三角波噪声值 / the triangle wave noise value
     */
    static float triangle(float amplitude, float midpoint, float frequency, float phaseShift, float q) {
        float p = phaseShift + frequency * q;
        return midpoint + amplitude * (Math.abs(2f * p + 1f - 4f * NoiseUtil.fastFloor(p / 2f + 0.75f)) - 1f);
    }


    /**
     * 将此一维噪声扩展为二维噪声，仅使用Y坐标作为输入。
     * <p>
     * Extends this 1D noise to a 2D noise, using only the Y coordinate as input.
     *
     * @return 新的二维噪声函数 / a new 2D noise function
     */
    default INoise2D extendX() {
        return (x, y) -> noise(y);
    }

    /**
     * 将此一维噪声扩展为二维噪声，仅使用X坐标作为输入。
     * <p>
     * Extends this 1D noise to a 2D noise, using only the X coordinate as input.
     *
     * @return 新的二维噪声函数 / a new 2D noise function
     */
    default INoise2D extendY() {
        return (x, y) -> noise(x);
    }

    /**
     * 计算给定输入的噪声值。
     * <p>
     * Computes the noise value for the given input.
     *
     * @param in 输入值 / the input value
     * @return 噪声值 / the noise value
     */
    float noise(float in);

    /**
     * 对输入坐标应用变换。适用于输入坐标的夹紧/缩放操作。
     * <p>
     * Applies a transformation to the input coordinates. Useful for clamp/scale operations on the input coordinates.
     *
     * @param transform 输入变换函数 / the input transformation function
     * @return 新的噪声函数 / a new noise function
     */
    default INoise1D transformed(INoise1D transform) {
        return in -> INoise1D.this.noise(transform.noise(in));
    }
}
