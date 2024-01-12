/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.util;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ReferenceValue<T> {
    public T val;

    public ReferenceValue() {
        super();
    }

    public ReferenceValue(T val) {
        super();
        this.val = val;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReferenceValue other = (ReferenceValue) obj;
        if (val == null) {
            if (other.val != null)
                return false;
        } else if (!val.equals(other.val))
            return false;
        return true;
    }

    public T getVal() {
        return val;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((val == null) ? 0 : val.hashCode());
        return result;
    }

    public void map(UnaryOperator<T> oo) {
        val = oo.apply(val);
    }

    public void mapIfPresent(UnaryOperator<T> oo) {
        if (val != null)
            val = oo.apply(val);
    }

    public void setIfAbsent(Supplier<T> s) {
        if (val == null)
            val = s.get();
    }

    public void setVal(T val) {
        this.val = val;
    }

}
