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
 * 空变量环境，不包含任何变量。用于对纯常量表达式求值。
 * 调用get方法会抛出异常，getOptional返回null。
 * <p>
 * Null variable environment containing no variables. Used for evaluating pure constant expressions.
 * Calling get throws an exception, getOptional returns null.
 */
public class NullEnvironment implements IEnvironment {
    public static final IEnvironment INSTANCE = new NullEnvironment();

    private NullEnvironment() {
        super();
    }

    @Override
    public double get(String key) {
        throw new IllegalStateException("Connot call variant on non variant enironment.");
    }

    @Override
    public Double getOptional(String key) {
        return null;
    }

    @Override
    public void set(String key, double v) {

    }
}
