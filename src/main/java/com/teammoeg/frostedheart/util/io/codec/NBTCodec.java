package com.teammoeg.frostedheart.util.io.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.marshaller.ClassInfo;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;

public class NBTCodec<A extends NBTSerializable> implements Codec<A> {
	Class<A> clazz;
	
	public NBTCodec(Class<A> clazz) {
		super();
		this.clazz = clazz;
	}

	@Override
	public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
		DataResult<MapLike<T>> data=ops.getMap(NBTDynamicOps.INSTANCE.convertMap(ops, input.serializeNBT()));
		if(!data.result().isPresent())
			return (DataResult<T>) data;
		return ops.mergeToMap(prefix, data.result().get());
	}

	@Override
	public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {

		INBT nbt = ops.convertTo(NBTDynamicOps.INSTANCE, input);
		if (nbt instanceof CompoundNBT) {
			A inst = ClassInfo.createInstance(clazz);
			inst.deserializeNBT((CompoundNBT) nbt);
			return DataResult.success(Pair.of(inst, input));
		}
		return DataResult.error("Not A Compound");

	}



}
