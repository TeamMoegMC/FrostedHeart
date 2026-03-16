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

import java.util.function.Supplier;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * 空操作编解码器。编码时不输出任何数据，解码时始终返回预设值。适用于不需要持久化的常量或默认值。
 * <p>
 * No-operation codec. Outputs no data during encoding and always returns a preset value during decoding.
 * Suitable for constants or default values that do not need persistence.
 *
 * @param <A> 编解码器处理的类型 / the type handled by the codec
 */
public class NopCodec<A> implements Codec<A> {
	Supplier<A> generator;
	/**
	 * 使用固定值构造空操作编解码器。
	 * <p>
	 * Constructs a no-operation codec with a fixed value.
	 *
	 * @param value 解码时返回的固定值 / the fixed value returned during decoding
	 */
	public NopCodec(A value) {
		super();
		this.generator =()-> value;
	}
	/**
	 * 使用值提供器构造空操作编解码器。
	 * <p>
	 * Constructs a no-operation codec with a value supplier.
	 *
	 * @param generator 解码时返回值的提供器 / the supplier for the value returned during decoding
	 */
	public NopCodec(Supplier<A> generator) {
		super();
		this.generator = generator;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		return DataResult.success(ops.empty());
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
		return DataResult.success(Pair.of(generator.get(), input));
	}
	@Override
	public String toString() {
		return "NopCodec[" + generator + "]";
	}
	
}
