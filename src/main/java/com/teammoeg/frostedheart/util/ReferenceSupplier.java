package com.teammoeg.frostedheart.util;

import java.util.function.Supplier;

public class ReferenceSupplier<T> implements Supplier<T> {
    Supplier<T> ref;

    public ReferenceSupplier() {
    }

    @Override
    public T get() {

        return ref != null ? ref.get() : null;
    }

    public <E extends Supplier<T>> E set(E sup) {
        ref = sup;
        return sup;
    }

}
