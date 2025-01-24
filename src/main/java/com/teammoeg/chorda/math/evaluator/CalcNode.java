/*
 * Copyright (c) 2023-2024 TeamMoeg
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

import java.util.function.DoubleUnaryOperator;

class CalcNode implements Node {
    DoubleUnaryOperator calc;
    Node nested;
    String name;

    public CalcNode(Node nested, String name, DoubleUnaryOperator calc) {
        this.calc = calc;
        this.nested = nested;
        this.name = name;
    }

    @Override
    public double eval(IEnvironment env) {
        return calc.applyAsDouble(nested.eval(env));
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {
        nested = nested.simplify();
        if (nested.isPrimary()) {
            return new ConstNode(eval(NullEnvironment.INSTANCE));
        }
        return this;
    }

    @Override
    public String toString() {
        return "(" + name + nested + ")";
    }
}