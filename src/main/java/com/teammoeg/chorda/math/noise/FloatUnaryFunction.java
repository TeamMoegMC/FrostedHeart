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
 * float类型的一元函数接口，接受一个float参数并返回一个float结果。
 * <p>
 * A unary function interface for float type, accepting a float argument and returning a float result.
 */
@FunctionalInterface
public interface FloatUnaryFunction {
    /**
     * 对给定的float值应用此函数。
     * <p>
     * Applies this function to the given float value.
     *
     * @param f 输入值 / the input value
     * @return 函数结果 / the function result
     */
    float applyAsFloat(float f);
}