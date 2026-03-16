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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableObject;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.RecordBuilder;

/**
 * 离散列表编解码器。仅序列化非空元素及其索引位置，反序列化时根据索引恢复完整列表，空位用空元素填充。
 * <p>
 * Discrete list codec. Only serializes non-empty elements with their index positions,
 * and restores the full list by index during deserialization, filling gaps with empty elements.
 *
 * @param <A> 列表元素的类型 / the type of list elements
 */
public final class DiscreteListCodec<A> implements Codec<List<A>> {
    private final Codec<A> elementCodec;
    private final Predicate<A> empty;
    private final Supplier<A> emptyItem;
    private final String index;



	/**
	 * 构造一个离散列表编解码器。
	 * <p>
	 * Constructs a discrete list codec.
	 *
	 * @param elementCodec 元素的编解码器 / the codec for elements
	 * @param empty 判断元素是否为空的谓词 / the predicate to determine if an element is empty
	 * @param emptyItem 空元素的提供器 / the supplier for empty elements
	 * @param index 索引字段的键名 / the key name for the index field
	 */
	public DiscreteListCodec(Codec<A> elementCodec, Predicate<A> empty, Supplier<A> emptyItem, String index) {
		super();
		this.elementCodec = elementCodec;
		this.empty = empty;
		this.emptyItem = emptyItem;
		this.index = index;
	}

	@Override
    public <T> DataResult<T> encode(final List<A> input, final DynamicOps<T> ops, final T prefix) {
		if(ops.compressMaps()) {
			 final RecordBuilder<T> builder = ops.mapBuilder();
			 int i=0;
			 for (final A a : input) {
				 final int cri=i;
				 if(!empty.test(a))
					 builder.add(ops.createInt(i), elementCodec.encodeStart(ops, a));
				 i++;
			 }
			 return builder.build(prefix);
		}
        final ListBuilder<T> builder = ops.listBuilder();
        int i=0;
        for (final A a : input) {
        	final int cri=i;
        	if(!empty.test(a))
            	builder.add(elementCodec.encodeStart(ops, a).flatMap(o->ops.mergeToMap(o, ops.createString(index), ops.createInt(cri))));
            i++;
        }

        return builder.build(prefix);
    }

    @Override
    public <T> DataResult<Pair<List<A>, T>> decode(final DynamicOps<T> ops, final T input) {
    	if(ops.compressMaps()) {
    		return ops.getMap(input).flatMap(map->{
                List<A> list=new ArrayList<>();
                final Stream.Builder<T> failed = Stream.builder();
                final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));
    			map.entries().forEach(t->{
                    final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t.getSecond());
                    final DataResult<Number> key=ops.getNumberValue(t.getFirst());
                    element.error().ifPresent(e -> failed.add(t.getSecond()));
                    result.setValue(result.getValue().apply3((r,k, v) -> {
                    	addEmptyIndexBefore(list,k.intValue()+1);
                    	list.set(k.intValue(),v.getFirst());
                        return r;
                    }, key, element));
    			});
    			final T errors = ops.createList(failed.build());

                final Pair<List<A>, T> pair = Pair.of(list, errors);

                return result.getValue().map(unit -> pair).setPartial(pair);
    		});
    	}
        return ops.getList(input).flatMap(stream -> {
            List<A> list=new ArrayList<>();
            final Stream.Builder<T> failed = Stream.builder();
            final MutableObject<DataResult<Unit>> result = new MutableObject<>(DataResult.success(Unit.INSTANCE, Lifecycle.stable()));

            stream.accept(t -> {
                final DataResult<Pair<A, T>> element = elementCodec.decode(ops, t);
                final DataResult<Integer> key=ops.get(t, index).flatMap(o->ops.getNumberValue(o)).map(o->o.intValue());
                element.error().ifPresent(e -> failed.add(t));
                result.setValue(result.getValue().apply3((r,k, v) -> {
                	addEmptyIndexBefore(list,k+1);
                	list.set(k,v.getFirst());
                    return r;
                }, key, element));
            });

            final T errors = ops.createList(failed.build());

            final Pair<List<A>, T> pair = Pair.of(list, errors);

            return result.getValue().map(unit -> pair).setPartial(pair);
        });
    }
    private void addEmptyIndexBefore(List<A> list,int size) {
    	while(size>list.size()) {
    		list.add(emptyItem.get());
    	}
    }
    @Override
    public int hashCode() {
        return Objects.hash(elementCodec);
    }

    @Override
    public String toString() {
        return "ListCodec[" + elementCodec + ']';
    }
}
