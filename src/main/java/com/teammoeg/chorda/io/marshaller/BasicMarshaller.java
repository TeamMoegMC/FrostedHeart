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

package com.teammoeg.chorda.io.marshaller;

import java.util.function.Function;

import net.minecraft.nbt.Tag;

/**
 * 基础编组器。使用函数对在 NBT 标签和 Java 对象之间进行转换，支持默认值。
 * <p>
 * Basic marshaller. Converts between NBT tags and Java objects using function pairs, with optional default value support.
 *
 * @param <R> NBT 标签类型 / NBT tag type
 * @param <T> Java 对象类型 / Java object type
 */
public class BasicMarshaller<R extends Tag,T> implements Marshaller {
	final Function<R,T> from;
	final Function<T,R> to;
	final Class<R> cls;
	final T def;
	/**
	 * 构造一个无默认值的基础编组器。
	 * <p>
	 * Constructs a basic marshaller without a default value.
	 *
	 * @param nbtcls NBT 标签的类对象 / Class object of the NBT tag
	 * @param from NBT 转对象的函数 / Function to convert from NBT to object
	 * @param to 对象转 NBT 的函数 / Function to convert from object to NBT
	 */
	public BasicMarshaller(Class<R> nbtcls,Function<R, T> from, Function<T, R> to) {
		this(nbtcls,from,to,null);
	}

	/**
	 * 构造一个带默认值的基础编组器。
	 * <p>
	 * Constructs a basic marshaller with a default value.
	 *
	 * @param cls NBT 标签的类对象 / Class object of the NBT tag
	 * @param from NBT 转对象的函数 / Function to convert from NBT to object
	 * @param to 对象转 NBT 的函数 / Function to convert from object to NBT
	 * @param def NBT 类型不匹配时返回的默认值 / Default value returned when NBT type does not match
	 */
	public BasicMarshaller(Class<R> cls,Function<R, T> from, Function<T, R> to, T def) {
		super();
		this.from = from;
		this.to = to;
		this.cls = cls;
		this.def = def;
	}

	/** {@inheritDoc} */
	@Override
	public Tag toNBT(Object o) {
		return to.apply((T) o);
	}

	/** {@inheritDoc} */
	@Override
	public Object fromNBT(Tag nbt) {
		if(cls.isInstance(nbt))
			return from.apply((R) nbt);
		return def;
	}

}
