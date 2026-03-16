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
 * 带类型返回值的二维噪声接口，返回指定类型而非float。
 * <p>
 * A typed 2D noise interface that returns a specified type instead of float.
 *
 * @param <T> 返回值类型 / the return type
 */
public interface ITyped2D<T> {
    /**
     * 计算给定二维坐标处的类型化值。
     * <p>
     * Computes the typed value at the given 2D coordinates.
     *
     * @param x X坐标 / the X coordinate
     * @param y Y坐标 / the Y coordinate
     * @return 计算结果 / the computed result
     */
    T typed(float x, float y);
}
