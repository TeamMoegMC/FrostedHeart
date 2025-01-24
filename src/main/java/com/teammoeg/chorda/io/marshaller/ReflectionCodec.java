package com.teammoeg.chorda.io.marshaller;

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class ReflectionCodec<A> extends MapCodec<A> {
	ClassInfo info;
	public ReflectionCodec(Class<A> info) {
		this.info=ClassInfo.valueOf(info);
	}
	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		CompoundTag nbt=new CompoundTag();
		input.entries().forEach(o->nbt.put(NbtOps.INSTANCE.getStringValue(ops.convertTo(NbtOps.INSTANCE, o.getFirst())).result().orElse(""), ops.convertTo(NbtOps.INSTANCE, o.getSecond())));
		return DataResult.success((A)info.fromNBT(nbt));
	}
	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		CompoundTag nbt=(CompoundTag) info.toNBT(input);
		for(String in:nbt.getAllKeys())
			prefix.add(in, NbtOps.INSTANCE.convertTo(ops, nbt.get(in)));
		return prefix;
	}
	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return info.getFields().map(o->o.name).map(ops::createString);
	}
}
