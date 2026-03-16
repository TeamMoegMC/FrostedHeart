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

package com.teammoeg.chorda.math.evaluator;

/**
 * 表达式求值的变量环境接口，提供变量的读取和设置功能。
 * <p>
 * Variable environment interface for expression evaluation, providing variable read and write operations.
 */
public interface IEnvironment {
    /**
     * 获取指定变量的值。如果变量不存在，行为由实现决定。
     * <p>
     * Gets the value of the specified variable. Behavior when variable does not exist is implementation-defined.
     *
     * @param key 变量名 / the variable name
     * @return 变量值 / the variable value
     */
    double get(String key);

    /**
     * 可选地获取指定变量的值。如果变量不存在则返回null。
     * <p>
     * Optionally gets the value of the specified variable. Returns null if the variable does not exist.
     *
     * @param key 变量名 / the variable name
     * @return 变量值，或null / the variable value, or null
     */
    Double getOptional(String key);

    /**
     * 设置指定变量的值。
     * <p>
     * Sets the value of the specified variable.
     *
     * @param key 变量名 / the variable name
     * @param v 要设置的值 / the value to set
     */
    void set(String key, double v);
}
