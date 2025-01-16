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

package com.teammoeg.chorda.util.utility;

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
