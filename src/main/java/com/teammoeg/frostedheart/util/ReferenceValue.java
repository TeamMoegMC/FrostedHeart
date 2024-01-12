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

    public T getVal() {
        return val;
    }

    public void setVal(T val) {
        this.val = val;
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

}
