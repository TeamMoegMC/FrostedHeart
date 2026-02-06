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

public class ConstantEnvironment implements IEnvironment {
    Map<String, Double> map;

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
