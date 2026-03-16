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

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

/**
 * 函数调用节点，表示对多个参数执行命名函数运算。
 * 支持确定性函数的常量折叠优化。
 * <p>
 * Function call node representing a named function operation on multiple arguments.
 * Supports constant folding optimization for deterministic functions.
 */
class FuncCallNode implements Node {
    ToDoubleFunction<double[]> calc;
    Node[] nested;
    String name;
    boolean isDeterministic;

    /**
     * 构造一个函数调用节点。
     * <p>
     * Constructs a function call node.
     *
     * @param nested 参数节点数组 / the array of parameter nodes
     * @param name 函数名 / the function name
     * @param paramCount 期望的参数数量 / the expected parameter count
     * @param calc 函数计算逻辑 / the function computation logic
     * @param isDeterministic 是否为确定性函数（相同输入始终产生相同输出） / whether the function is deterministic (same inputs always produce same outputs)
     * @throws RuntimeException 如果实际参数数量与期望不符 / if actual parameter count does not match expected
     */
    public FuncCallNode(Node[] nested, String name, int paramCount, ToDoubleFunction<double[]> calc, boolean isDeterministic) {
        this.calc = calc;
        this.nested = nested;
        this.name = name;
        this.isDeterministic = isDeterministic;
        if (nested.length != paramCount) {
            throw new RuntimeException("Bad param count for " + name + " expected " + paramCount + " but got " + nested.length);
        }
    }

    @Override
    public double eval(IEnvironment env) {
        double[] par = new double[nested.length];
        for (int i = 0; i < par.length; i++) {
            par[i] = nested[i].eval(env);
        }
        return calc.applyAsDouble(par);
    }

    @Override
    public boolean isPrimary() {
        if (!isDeterministic)
            return false;
        boolean isPrime = true;
        for (Node node : nested) {
            isPrime &= node.isPrimary();
        }
        return isPrime;
    }

    @Override
    public Node simplify() {
        for (int i = 0; i < nested.length; i++) {
            nested[i] = nested[i].simplify();
        }
        if (isPrimary()) {
            return new ConstNode(eval(NullEnvironment.INSTANCE));
        }
        return this;
    }

    @Override
    public String toString() {

        return name + "(" + Arrays.toString(nested) + ")";
    }
}