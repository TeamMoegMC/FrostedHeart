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

package com.teammoeg.chorda.io;

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * 带工厂方法的Codec包装器，在解码失败时使用工厂方法提供默认实例。
 * <p>
 * A Codec wrapper with a factory supplier that provides a default instance when decoding fails.
 *
 * @param <A> 编解码的对象类型 / the type of object being encoded and decoded
 * @param codec 底层编解码器 / the underlying codec
 * @param factory 默认实例工厂 / the factory supplier for default instances
 */
public record CodecWithFactory<A>(Codec<A> codec, Supplier<A> factory) implements Codec<A>{

	/**
	 * 通过工厂方法获取一个新实例。
	 * <p>
	 * Gets a new instance from the factory supplier.
	 *
	 * @return 工厂创建的新实例 / a new instance created by the factory
	 */
	public A getInstance() {
		return factory.get();
	}
	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return codec.encode( input,ops, prefix);
	}


	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(codec.parse(ops, input).result().orElseGet(factory),input));
	}


}
