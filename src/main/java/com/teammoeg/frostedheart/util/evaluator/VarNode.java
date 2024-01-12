/*
 * Copyright (c) 2023 IEEM Trivium Society/khjxiaogu
 *
 * This file is part of Convivium.
 *
 * Convivium is free software: you can redistribute it and/or modify
 * it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 * the Free Software Foundation, version 3.
 *
 * Convivium is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU LESSER GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 * along with Convivium. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.util.evaluator;

class VarNode implements Node {
    String token;

    public VarNode(String token) {
        this.token = token;
    }

    @Override
    public double eval(IEnvironment env) {
        return env.get(token);
    }

    @Override
    public String toString() {
        return token;
    }

    @Override
    public boolean isPrimary() {
        return false;
    }

    @Override
    public Node simplify() {
        return this;
    }
}