package com.teammoeg.frostedheart.util.io.marshaller;

import java.util.stream.Stream;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;

public class ReflectionCodec<A> extends MapCodec<A> {
	ClassInfo info;
	public ReflectionCodec(Class<A> info) {
		this.info=ClassInfo.valueOf(info);
	}
	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		CompoundNBT nbt=new CompoundNBT();
		input.entries().forEach(o->nbt.put(NBTDynamicOps.INSTANCE.getStringValue(ops.convertTo(NBTDynamicOps.INSTANCE, o.getFirst())).result().orElse(""), ops.convertTo(NBTDynamicOps.INSTANCE, o.getSecond())));
		return DataResult.success((A)info.fromNBT(nbt));
	}
	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		CompoundNBT nbt=(CompoundNBT) info.toNBT(input);
		for(String in:nbt.getAllKeys())
			prefix.add(in, NBTDynamicOps.INSTANCE.convertTo(ops, nbt.get(in)));
		return prefix;
	}
	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return info.getFields().map(o->o.name).map(ops::createString);
	}
}
