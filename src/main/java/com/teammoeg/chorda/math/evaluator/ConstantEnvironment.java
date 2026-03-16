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

import java.util.Map;

/**
 * 只读的常量变量环境，变量值在构造时固定，不支持修改。
 * <p>
 * A read-only constant variable environment where variable values are fixed at construction time
 * and cannot be modified.
 */
public class ConstantEnvironment implements IEnvironment {
    Map<String, Double> map;

    /**
     * 使用指定的变量映射构造常量环境。
     * <p>
     * Constructs a constant environment with the specified variable map.
     *
     * @param map 变量名到值的映射 / the variable name to value map
     */
    public ConstantEnvironment(Map<String, Double> map) {
        super();
        this.map = map;
    }

    @Override
    public double get(String key) {
        Double d = getOptional(key);
        return d == null ? 0 : d;
    }

    @Override
    public Double getOptional(String key) {
        return map.get(key);
    }


    @Override
    public void set(String key, double v) {
        throw new IllegalStateException("Connot set variant on constant enironment.");
    }

}
