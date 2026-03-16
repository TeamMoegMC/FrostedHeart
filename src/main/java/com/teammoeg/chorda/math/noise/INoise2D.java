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
 * 二维噪声层的封装接口，支持绝对值、叠加、裁剪、八度叠加、域扭曲等丰富的噪声操作。
 * <p>
 * Wrapper interface for a 2D noise layer, supporting absolute value, addition, flattening, octave stacking,
 * domain warping, and other rich noise operations.
 */
@FunctionalInterface
public interface INoise2D {
    /**
     * 对噪声函数取绝对值，不缩放结果。
     * <p>
     * Takes the absolute value of a noise function. Does not scale the result.
     *
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D abs() {
        return (x, y) -> Math.abs(INoise2D.this.noise(x, y));
    }

    /**
     * 将此噪声与另一个噪声相加。
     * <p>
     * Adds this noise to another noise.
     *
     * @param other 另一个噪声函数 / the other noise function
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D add(INoise2D other) {
        return (x, y) -> INoise2D.this.noise(x, y) + other.noise(x, y);
    }

    /**
     * 通过截断超出阈值的值来创建平坦化噪声。
     * <p>
     * Creates flattened noise by cutting off values above or below a threshold.
     *
     * @param min 最小噪声值 / the minimum noise value
     * @param max 最大噪声值 / the maximum noise value
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D flattened(float min, float max) {
        return (x, y) -> Mth.clamp(INoise2D.this.noise(x, y), min, max);
    }

    /**
     * 对噪声输出应用映射函数。
     * <p>
     * Applies a mapping function to the noise output.
     *
     * @param mappingFunction 映射函数 / the mapping function
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D map(FloatUnaryFunction mappingFunction) {
        return (x, y) -> mappingFunction.applyAsFloat(INoise2D.this.noise(x, y));
    }

    /**
     * 计算给定二维坐标处的噪声值。
     * <p>
     * Computes the noise value at the given 2D coordinates.
     *
     * @param x X坐标 / the X coordinate
     * @param z Z坐标 / the Z coordinate
     * @return 噪声值 / the noise value
     */
    float noise(float x, float z);

    /**
     * 使用默认持续度0.5创建简单八度叠加噪声。
     * <p>
     * Creates simple octave noise using the default persistence of 0.5.
     *
     * @param octaves 八度数量 / the number of octaves
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D octaves(int octaves) {
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
    default INoise2D octaves(int octaves, float persistence) {
        final float[] frequency = new float[octaves];
        final float[] amplitude = new float[octaves];
        for (int i = 0; i < octaves; i++) {
            frequency[i] = 1 << i;
            amplitude[i] = (float) Math.pow(persistence, octaves - i);
        }
        return (x, y) -> {
            float value = 0;
            for (int i = 0; i < octaves; i++) {
                value += INoise2D.this.noise(x / frequency[i], y / frequency[i]) * amplitude[i];
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
    default INoise2D ridged() {
        return (x, y) -> {
            float value = INoise2D.this.noise(x, y);
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
    default INoise2D scaled(float min, float max) {
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
    default INoise2D scaled(float oldMin, float oldMax, float min, float max) {
        final float scale = (max - min) / (oldMax - oldMin);
        final float shift = min - oldMin * scale;
        return (x, y) -> INoise2D.this.noise(x, y) * scale + shift;
    }

    /**
     * 通过缩放输入参数来扩展噪声。
     * <p>
     * Spreads out the noise by scaling the input parameters.
     *
     * @param scaleFactor 输入参数的缩放因子 / the scale factor for the input parameters
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D spread(float scaleFactor) {
        return (x, y) -> INoise2D.this.noise(x * scaleFactor, y * scaleFactor);
    }

    /**
     * 通过取最近的级别并四舍五入来创建"阶梯"效果。输入必须在[-1, 1]范围内。
     * <p>
     * Creates "terraces" by taking the nearest level and rounding. Input must be in range [-1, 1].
     *
     * @param levels 要四舍五入到的级别数 / the number of levels to round to
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D terraces(int levels) {
        return (x, y) -> {
            float value = 0.5f * INoise2D.this.noise(x, y) + 0.5f;
            float rounded = (int) (value * levels); // In range [0, levels)
            return (rounded * 2f) / levels - 1f;
        };
    }

    /**
     * 对输入坐标应用变换。与{@link INoise2D#warped(INoise2D, INoise2D)}类似，但不将值累加到坐标上，
     * 而是直接替换坐标，适用于输入坐标的夹紧/缩放操作。
     * <p>
     * Applies a transformation to the input coordinates. Similar to {@link INoise2D#warped(INoise2D, INoise2D)}
     * but replaces coordinates instead of adding to them, making it useful for clamp/scale operations.
     *
     * @param transformX X坐标变换 / the X coordinate transformation
     * @param transformY Y坐标变换 / the Y coordinate transformation
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D transformed(INoise2D transformX, INoise2D transformY) {
        return (x, y) -> INoise2D.this.noise(transformX.noise(x, y), transformY.noise(x, y));
    }

    /**
     * 使用两个噪声函数对每个输入坐标施加域扭曲。
     * <p>
     * Applies domain warping to each input coordinate using two noise functions.
     *
     * @param warpX X方向扭曲噪声 / the X-direction warp noise
     * @param warpY Y方向扭曲噪声 / the Y-direction warp noise
     * @return 新的噪声函数 / a new noise function
     */
    default INoise2D warped(INoise2D warpX, INoise2D warpY) {
        return (x, y) -> INoise2D.this.noise(x + warpX.noise(x, y), y + warpY.noise(x, y));
    }
}