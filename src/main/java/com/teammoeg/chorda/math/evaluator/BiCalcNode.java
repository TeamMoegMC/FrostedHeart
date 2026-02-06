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

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

class BiCalcNode extends BiNode {
    public static Map<DoubleBinaryOperator, String> toStr = new HashMap<>();
    public static DoubleBinaryOperator add = Double::sum;
    public static DoubleBinaryOperator min = (v1, v2) -> v1 - v2;
    public static DoubleBinaryOperator mul = (v1, v2) -> v1 * v2;
    public static DoubleBinaryOperator div = (v1, v2) -> v1 / v2;
    public static DoubleBinaryOperator sdiv = (v1, v2) -> v2 == 0 ? 0 : v1 / v2;
    public static DoubleBinaryOperator pow = Math::pow;
    public static DoubleBinaryOperator mod = (v1, v2) -> v1 % v2;
    static {
        toStr.put(add, "+");
        toStr.put(min, "-");
        toStr.put(mul, "*");
        toStr.put(div, "/");
        toStr.put(sdiv, "\\");
        toStr.put(pow, "^");
        toStr.put(mod, "%");
    }

    DoubleBinaryOperator calc;

    public BiCalcNode(Node left, Node right, DoubleBinaryOperator calc) {
        super(left, right);
        this.calc = calc;
    }

    @Override
    public double eval(IEnvironment env) {
        return calc.applyAsDouble(left.eval(env), right.eval(env));
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {
        left = left.simplify();
        right = right.simplify();
        if (left.isPrimary() && right.isPrimary())
            return new ConstNode(eval(NullEnvironment.INSTANCE));
        else if (calc == add || calc == min) {
            return new ExprNode(calc == add, left, right).simplify();
        } else if (calc == mul || calc == div) {
            return new TermNode(calc == mul, left, right).simplify();
        }
        return this;
    }

    @Override
    public String toString() {
        String cn = toStr.getOrDefault(calc, "" + calc);
        return left + cn + right;
    }

}