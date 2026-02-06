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

package com.teammoeg.chorda.io.codec;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableObject;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;

public final class StreamCodec<A> implements Codec<Stream<A>> {
    private final Codec<A> elementCodec;

    public StreamCodec(final Codec<A> elementCodec) {
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<T> encode(final Stream<A> input, final DynamicOps<T> ops, final T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();
        input.forEach(a->builder.add(elementCodec.encodeStart(ops, a)));

        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<Stream<A>, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getList(input).setLifecycle(Lifecycle.stable()).flatMap(stream -> {
            final ImmutableList.Builder<A> read = ImmutableList.builder();
            final Stream.Builder<T> failed = Stream.builder();
            // TODO: AtomicReference.getPlain/setPlain in java9+
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

            stream.accept(t -> {
                final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t);
                element.error().ifPresent(e -> failed.add(t));
                result.setValue(result.getValue().apply2stable((r, v) -> {
                    read.add(v.getFirst());
                    return r;
                }, element));
            });

            final ImmutableList<A> elements = read.build();
            final T errors = ops.createList(failed.build());

            final Pair<Stream<A>, T> pair = Pair.of(elements.stream(), errors);

            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final StreamCodec<?> listCodec = (StreamCodec<?>) o;
        return Objects.equals(elementCodec, listCodec.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementCodec);
    }

    @Override
    public String toString() {
        return "StreamCodec[" + elementCodec + ']';
    }
}
