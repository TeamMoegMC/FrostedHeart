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

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;

/**
 * 自定义列表编解码器。允许使用自定义的集合工厂来创建集合实例，而非默认的List。
 * <p>
 * Custom list codec. Allows using a custom collection factory to create collection instances
 * instead of the default List.
 *
 * @param <A> 集合元素的类型 / the type of collection elements
 * @param <L> 集合的类型 / the type of the collection
 */
public class CustomListCodec<A,L extends Collection<A>> implements Codec<L> {
    private final Codec<A> elementCodec;
    private final Supplier<L> factory;
    /**
     * 构造一个自定义列表编解码器。
     * <p>
     * Constructs a custom list codec.
     *
     * @param elementCodec 元素的编解码器 / the codec for elements
     * @param factory 集合实例的工厂 / the factory for collection instances
     */
    public CustomListCodec(final Codec<A> elementCodec,Supplier<L> factory) {
        this.elementCodec = elementCodec;
        this.factory=factory;
    }

    @Override
    public <T> DataResult<T> encode(final L input, final DynamicOps<T> ops, final T prefix) {
        final ListBuilder<T> builder = ops.listBuilder();
        for (final A a : input) {
            builder.add(elementCodec.encodeStart(ops, a));
        }
        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<L, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getList(input).flatMap(t->{
        	L obj=factory.get();
        	StringBuilder sb=new StringBuilder();
        	t.accept(o->{
        		DataResult<Pair<A,T>> a=elementCodec.decode(ops,input);
        		Pair<A,T> at=a.getOrThrow(true, sb::append);
        		obj.add(at.getFirst());
        	});
        	if(sb.length()!=0)
        		return DataResult.success(Pair.of(obj, input));
        	return DataResult.error(sb::toString,Pair.of(obj, input));
        });
    }


    @Override
	public int hashCode() {
		return Objects.hash(elementCodec, factory);
	}

    @Override
    public String toString() {
        return "CustomListCodec[" + elementCodec + ']';
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CustomListCodec other = (CustomListCodec) obj;
		return Objects.equals(elementCodec, other.elementCodec) && Objects.equals(factory, other.factory);
	}
}
