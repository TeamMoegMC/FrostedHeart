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

public class BooleansCodec extends MapCodec<boolean[]> {
	public static class BooleanCodecBuilder<O>{
		String flag;
		List<Pair<String,Function<O, Boolean>>> flags=new ArrayList<>();
		public BooleanCodecBuilder(String flag) {
			super();
			this.flag = flag;
		}
		public BooleanCodecBuilder<O> flag(String key,Function<O, Boolean> getter){
			flags.add(Pair.of(key, getter));
			return this;
		}
	    public RecordCodecBuilder<O, boolean[]> build() {
	        return new BooleansCodec(flag,flags.stream().map(t->t.getFirst()).toArray(String[]::new))
	        	.forGetter(flags.stream().map(t->t.getSecond()).toArray(Function[]::new));
	    }
	}
	String altkey;
	String[] keys;

	public BooleansCodec(String altkey, String... keys) {
		super();
		this.altkey = altkey;
		this.keys = keys;
	}
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o)}, this);
    }
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2,Function<O, Boolean> f3) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o),f3.apply(o)}, this);
    }
    public <O> RecordCodecBuilder<O, boolean[]> forGetter(Function<O, Boolean> f1,Function<O, Boolean> f2,Function<O, Boolean> f3,Function<O, Boolean> f4) {
        return RecordCodecBuilder.of(o->new boolean[] {f1.apply(o),f2.apply(o),f3.apply(o),f4.apply(o)}, this);
    }
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
