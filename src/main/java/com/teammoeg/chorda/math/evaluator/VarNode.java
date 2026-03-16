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
 * 变量引用节点，通过变量名从环境中查找对应的值。
 * <p>
 * Variable reference node that looks up the corresponding value by variable name from the environment.
 */
class VarNode implements Node {
    String token;

    /**
     * 构造一个变量引用节点。
     * <p>
     * Constructs a variable reference node.
     *
     * @param token 变量名 / the variable name
     */
    public VarNode(String token) {
        this.token = token;
    }

    @Override
    public double eval(IEnvironment env) {
        return env.get(token);
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {
        return this;
    }

    @Override
    public String toString() {
        return token;
    }
}