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

import net.minecraft.util.Mth;

/**
 * 三维噪声层的封装接口，支持绝对值、叠加、八度叠加、域扭曲等噪声操作。
 * <p>
 * Wrapper interface for a 3D noise layer, supporting absolute value, addition, octave stacking,
 * domain warping, and other noise operations.
 */
@FunctionalInterface
public interface INoise3D {
    /**
     * 对噪声函数取绝对值，不缩放结果。
     * <p>
     * Takes the absolute value of a noise function. Does not scale the result.
     *
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D abs() {
        return (x, y, z) -> Math.abs(INoise3D.this.noise(x, y, z));
    }

    /**
     * 将此噪声与另一个噪声相加。
     * <p>
     * Adds this noise to another noise.
     *
     * @param other 另一个噪声函数 / the other noise function
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D add(INoise3D other) {
        return (x, y, z) -> INoise3D.this.noise(x, y, z) + other.noise(x, y, z);
    }

    /**
     * 通过截断超出阈值的值来创建平坦化噪声。
     * <p>
     * Creates flattened noise by clamping values to the specified range.
     *
     * @param min 最小噪声值 / the minimum noise value
     * @param max 最大噪声值 / the maximum noise value
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D flattened(float min, float max) {
        return (x, y, z) -> Mth.clamp(INoise3D.this.noise(x, y, z), min, max);
    }

    /**
     * 对噪声输出应用映射函数。
     * <p>
     * Applies a mapping function to the noise output.
     *
     * @param mappingFunction 映射函数 / the mapping function
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D map(FloatUnaryFunction mappingFunction) {
        return (x, y, z) -> mappingFunction.applyAsFloat(INoise3D.this.noise(x, y, z));
    }

    /**
     * 计算给定三维坐标处的噪声值。
     * <p>
     * Computes the noise value at the given 3D coordinates.
     *
     * @param x X坐标 / the X coordinate
     * @param y Y坐标 / the Y coordinate
     * @param z Z坐标 / the Z coordinate
     * @return 噪声值 / the noise value
     */
    float noise(float x, float y, float z);

    /**
     * 使用默认持续度0.5创建简单八度叠加噪声。
     * <p>
     * Creates simple octave noise using the default persistence of 0.5.
     *
     * @param octaves 八度数量 / the number of octaves
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D octaves(int octaves) {
        return octaves(octaves, 0.5f);
    }

    /**
     * 创建简单八度叠加噪声，所有八度使用相同的基础噪声函数。
     * <p>
     * Creates simple octave noise where all octaves use the same base noise function.
     *
     * @param octaves 八度数量 / the number of octaves
     * @param persistence 每个八度的振幅基数 / the base for each octave's amplitude
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D octaves(int octaves, float persistence) {
        final float[] frequency = new float[octaves];
        final float[] amplitude = new float[octaves];
        for (int i = 0; i < octaves; i++) {
            frequency[i] = 1 << i;
            amplitude[i] = (float) Math.pow(persistence, octaves - i);
        }
        return (x, y, z) -> {
            float value = 0;
            for (int i = 0; i < octaves; i++) {
                value += INoise3D.this.noise(x / frequency[i], y / frequency[i], z / frequency[i]) * amplitude[i];
            }
            return value;
        };
    }

    /**
     * 使用绝对值创建山脊噪声。
     * <p>
     * Creates ridged noise using absolute value.
     *
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D ridged() {
        return (x, y, z) -> {
            float value = INoise3D.this.noise(x, y, z);
            value = value < 0 ? -value : value;
            return 1f - 2f * value;
        };
    }

    /**
     * 将噪声输出从默认范围[-1, 1]重新缩放到新范围。
     * <p>
     * Re-scales the noise output from the default range [-1, 1] to a new range.
     *
     * @param min 新的最小值 / the new minimum value
     * @param max 新的最大值 / the new maximum value
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D scaled(float min, float max) {
        return scaled(-1, 1, min, max);
    }

    /**
     * 将噪声输出从旧范围重新缩放到新范围。
     * <p>
     * Re-scales the output of the noise from an old range to a new range.
     *
     * @param oldMin 旧的最小值（通常为-1） / the old minimum value (typically -1)
     * @param oldMax 旧的最大值（通常为1） / the old maximum value (typically 1)
     * @param min 新的最小值 / the new minimum value
     * @param max 新的最大值 / the new maximum value
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D scaled(float oldMin, float oldMax, float min, float max) {
        return (x, y, z) -> {
            float value = INoise3D.this.noise(x, y, z);
            return (value - oldMin) / (oldMax - oldMin) * (max - min) + min;
        };
    }

    /**
     * 通过缩放输入参数来扩展噪声。
     * <p>
     * Spreads out the noise by scaling the input parameters.
     *
     * @param scaleFactor 输入参数的缩放因子 / the scale factor for the input parameters
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D spread(float scaleFactor) {
        return (x, y, z) -> INoise3D.this.noise(x * scaleFactor, y * scaleFactor, z * scaleFactor);
    }

    /**
     * 通过取最近的级别并四舍五入来创建"阶梯"效果。
     * <p>
     * Creates "terraces" by taking the nearest level and rounding.
     *
     * @param levels 要四舍五入到的级别数 / the number of levels to round to
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D terraces(int levels) {
        return (x, y, z) -> {
            float value = 0.5f * INoise3D.this.noise(x, y, z) + 0.5f;
            float rounded = (int) (value * levels); // In range [0, levels)
            return (rounded * 2f) / levels - 1f;
        };
    }

    /**
     * 使用三个噪声函数对每个输入坐标施加域扭曲。
     * <p>
     * Applies domain warping to each input coordinate using three noise functions.
     *
     * @param warpX X方向扭曲噪声 / the X-direction warp noise
     * @param warpY Y方向扭曲噪声 / the Y-direction warp noise
     * @param warpZ Z方向扭曲噪声 / the Z-direction warp noise
     * @return 新的噪声函数 / a new noise function
     */
    default INoise3D warped(INoise3D warpX, INoise3D warpY, INoise3D warpZ) {
        return (x, y, z) -> {
            float x0 = x + warpX.noise(x, y, z);
            float y0 = y + warpY.noise(x, y, z);
            float z0 = z + warpZ.noise(x, y, z);
            return INoise3D.this.noise(x0, y0, z0);
        };
    }
}