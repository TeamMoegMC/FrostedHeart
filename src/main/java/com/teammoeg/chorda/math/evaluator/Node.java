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
 * 数学表达式树的节点接口，支持求值、判断是否为常量以及化简操作。
 * <p>
 * Node interface for mathematical expression trees, supporting evaluation, primary (constant) detection,
 * and simplification operations.
 */
public interface Node {
    /**
     * 在给定的变量环境中对此节点求值。
     * <p>
     * Evaluates this node in the given variable environment.
     *
     * @param env 变量环境 / the variable environment
     * @return 求值结果 / the evaluation result
     */
    double eval(IEnvironment env);

    /**
     * 检查此节点是否为常量节点（不依赖任何变量）。
     * <p>
     * Checks whether this node is a primary (constant) node that does not depend on any variables.
     *
     * @return 如果是常量节点返回true / true if this is a constant node
     */
    boolean isPrimary();

    /**
     * 对此节点进行代数化简，尽可能将常量子表达式折叠为单个常量。
     * <p>
     * Algebraically simplifies this node, folding constant sub-expressions into single constants where possible.
     *
     * @return 化简后的节点 / the simplified node
     */
    Node simplify();
}