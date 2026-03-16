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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.SerializeUtil;

/**
 * 布尔数组的MapCodec实现。支持在压缩模式下将多个布尔值打包为字节/短整型/整型/位集，在非压缩模式下使用独立的键名存储。
 * <p>
 * MapCodec implementation for boolean arrays. Supports packing multiple booleans into
 * byte/short/int/BitSet in compressed mode, and storing with individual key names in uncompressed mode.
 */
public class BooleansCodec extends MapCodec<boolean[]> {
	/**
	 * 布尔编解码器构建器，用于通过流式API定义布尔标志位。
	 * <p>
	 * Boolean codec builder for defining boolean flags via a fluent API.
	 *
	 * @param <O> 拥有这些布尔字段的对象类型 / the object type that owns these boolean fields
	 */
	public static class BooleanCodecBuilder<O>{
		String flag;
		List<Pair<String,Function<O, Boolean>>> flags=new ArrayList<>();
		/**
		 * 构造布尔编解码器构建器。
		 * <p>
		 * Constructs a boolean codec builder.
		 *
		 * @param flag 压缩模式下的备用键名 / the alternative key name for compressed mode
		 */
		public BooleanCodecBuilder(String flag) {
			super();
			this.flag = flag;
		}
		/**
		 * 添加一个布尔标志位。
		 * <p>
		 * Adds a boolean flag.
		 *
		 * @param key 标志位的键名 / the key name for this flag
		 * @param getter 从对象获取布尔值的函数 / the function to get the boolean value from the object
		 * @return 此构建器实例 / this builder instance
		 */
		public BooleanCodecBuilder<O> flag(String key,Function<O, Boolean> getter){
			flags.add(Pair.of(key, getter));
			return this;
		}
		/**
		 * 构建RecordCodecBuilder。
		 * <p>
		 * Builds the RecordCodecBuilder.
		 *
		 * @return 构建的RecordCodecBuilder / the built RecordCodecBuilder
		 */
	    public RecordCodecBuilder<O, boolean[]> build() {
	        return new BooleansCodec(flag,flags.stream().map(t->t.getFirst()).toArray(String[]::new))
	        	.forGetter(flags.stream().map(t->t.getSecond()).toArray(Function[]::new));
	    }
	}
	String altkey;
	String[] keys;

	/**
	 * 构造布尔数组编解码器。
	 * <p>
	 * Constructs a booleans codec.
	 *
	 * @param altkey 压缩模式下使用的备用键名 / the alternative key name used in compressed mode
	 * @param keys 每个布尔值对应的键名数组 / the key name array for each boolean value
	 */
	public BooleansCodec(String altkey, String... keys) {
		super();
		this.altkey = altkey;
		this.keys = keys;
	}
    /**
     * 为两个布尔字段创建RecordCodecBuilder。
     * <p>
     * Creates a RecordCodecBuilder for two boolean fields.
     *
     * @param f1 第一个布尔值的获取函数 / getter for the first boolean
     * @param f2 第二个布尔值的获取函数 / getter for the second boolean
     * @param <O> 拥有这些字段的对象类型 / the object type owning these fields
     * @return RecordCodecBuilder实例 / the RecordCodecBuilder instance
     */
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o)}, this);
    }
    /**
     * 为三个布尔字段创建RecordCodecBuilder。
     * <p>
     * Creates a RecordCodecBuilder for three boolean fields.
     *
     * @param f1 第一个布尔值的获取函数 / getter for the first boolean
     * @param f2 第二个布尔值的获取函数 / getter for the second boolean
     * @param f3 第三个布尔值的获取函数 / getter for the third boolean
     * @param <O> 拥有这些字段的对象类型 / the object type owning these fields
     * @return RecordCodecBuilder实例 / the RecordCodecBuilder instance
     */
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2,Function<O, Boolean> f3) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o),f3.apply(o)}, this);
    }
    /**
     * 为四个布尔字段创建RecordCodecBuilder。
     * <p>
     * Creates a RecordCodecBuilder for four boolean fields.
     *
     * @param f1 第一个布尔值的获取函数 / getter for the first boolean
     * @param f2 第二个布尔值的获取函数 / getter for the second boolean
     * @param f3 第三个布尔值的获取函数 / getter for the third boolean
     * @param f4 第四个布尔值的获取函数 / getter for the fourth boolean
     * @param <O> 拥有这些字段的对象类型 / the object type owning these fields
     * @return RecordCodecBuilder实例 / the RecordCodecBuilder instance
     */
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2,Function<O, Boolean> f3,Function<O, Boolean> f4) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o),f3.apply(o),f4.apply(o)}, this);
    }
    /**
     * 为可变数量的布尔字段创建RecordCodecBuilder。
     * <p>
     * Creates a RecordCodecBuilder for a variable number of boolean fields.
     *
     * @param fs 布尔值获取函数数组 / array of boolean getter functions
     * @param <O> 拥有这些字段的对象类型 / the object type owning these fields
     * @return RecordCodecBuilder实例 / the RecordCodecBuilder instance
     */
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean>... fs) {
        return RecordCodecBuilder.of(o->{
        	boolean[] bools=new boolean[fs.length];
        	for(int i=0;i<bools.length;i++) {
        		bools[i]=fs[i].apply(o);
        	}
        	return bools;
        }, this);
    }
	@Override
	public <T> DataResult<boolean[]> decode(DynamicOps<T> ops, MapLike<T> input) {
		if(ops.compressMaps()) {
			if(keys.length<=8)
				return Codec.BYTE.decode(ops,input.get(altkey)).map(t-> SerializeUtil.readBooleans(t.getFirst()));
			if(keys.length<=16)
				return Codec.SHORT.decode(ops,input.get(altkey)).map(t->SerializeUtil.readLongBooleans(t.getFirst(),keys.length));
			if(keys.length<=32)
				return Codec.INT.decode(ops,input.get(altkey)).map(t->SerializeUtil.readLongBooleans(t.getFirst(),keys.length));
			return Codec.BYTE_BUFFER.decode(ops, input.get(altkey)).map(t->BitSet.valueOf(t.getFirst())).map(o->{
				int len=keys.length;
				boolean[] bss=new boolean[len];
				for(int i=0;i<len;i++) {
					bss[i]=o.get(i);
				}
				return bss;
			});
		}
		boolean[] bss=new boolean[keys.length];
		for(int i=0;i<keys.length;i++) {
			bss[i]=ops.getBooleanValue(input.get(keys[i])).resultOrPartial(s->{}).orElse(false);
		}
		return DataResult.success(bss);
	}

	@Override
	public <T> RecordBuilder<T> encode(boolean[] input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		if(ops.compressMaps()) {
			if(keys.length<=8)
				return prefix.add(altkey, Codec.BYTE.encodeStart(ops,SerializeUtil.writeBooleans(input)));
			if(keys.length<=16)
				return prefix.add(altkey, Codec.SHORT.encodeStart(ops,(short)SerializeUtil.writeLongBooleans(input)));
			if(keys.length<=32)
				return prefix.add(altkey, Codec.INT.encodeStart(ops,(int)SerializeUtil.writeLongBooleans(input)));
			BitSet bb=new BitSet(keys.length);
			for(int i=0;i<input.length;i++)
				bb.set(i, input[i]);
			return prefix.add(altkey, Codec.BYTE_BUFFER.encodeStart(ops,ByteBuffer.wrap(bb.toByteArray())));
		}
		int size=Math.min(input.length, keys.length);
		for(int i=0;i<size;i++) {
			if(input[i])
				prefix=prefix.add(keys[i], ops.createBoolean(input[i]));
		}
		return prefix;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		if(ops.compressMaps())
			return Stream.of(altkey).map(ops::createString);
		return Stream.of(keys).map(ops::createString);
	}

	@Override
	public String toString() {
		return "Booleans:" + altkey + "" + Arrays.toString(keys) + "";
	}

}
