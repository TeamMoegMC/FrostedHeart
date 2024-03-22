package com.teammoeg.frostedheart.util.io.codec;

import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.teammoeg.frostedheart.util.io.SerializeUtil;

public class BooleansCodec extends MapCodec<boolean[]> {
	String altkey;
	String[] keys;

	public BooleansCodec(String altkey, String... keys) {
		super();
		this.altkey = altkey;
		this.keys = keys;
	}

	@Override
	public <T> DataResult<boolean[]> decode(DynamicOps<T> ops, MapLike<T> input) {
		if(ops.compressMaps()) {
			return Codec.BYTE.decode(ops,input.get(altkey)).map(t->SerializeUtil.readBooleans(t.getFirst()));
		}
		boolean[] bss=new boolean[keys.length];
		for(int i=0;i<keys.length;i++) {
			bss[i]=ops.getBooleanValue(input.get(keys[i])).result().orElse(false);
		}
		return DataResult.success(bss);
	}

	@Override
	public <T> RecordBuilder<T> encode(boolean[] input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		if(ops.compressMaps()) {
			return prefix.add(altkey, Codec.BYTE.encodeStart(ops,SerializeUtil.writeBooleans(input)));
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

}
