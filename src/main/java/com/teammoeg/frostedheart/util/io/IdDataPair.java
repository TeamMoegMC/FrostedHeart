package com.teammoeg.frostedheart.util.io;

import com.mojang.serialization.MapCodec;
import com.teammoeg.frostedheart.util.io.codec.JointCodec;

import net.minecraft.util.ResourceLocation;

public class IdDataPair<T> {
	ResourceLocation id;
	T obj;
	public IdDataPair(ResourceLocation id, T obj) {
		super();
		this.id = id;
		this.obj = obj;
	}
	public ResourceLocation getId() {
		return id;
	}
	public T getObj() {
		return obj;
	}

	public static <A> MapCodec<IdDataPair<A>> createCodec(MapCodec<A> original){
		return new JointCodec<ResourceLocation,A,IdDataPair<A>>(ResourceLocation.CODEC.fieldOf("id"), original, IdDataPair::new, IdDataPair::getId, IdDataPair::getObj);
	}
	@Override
	public String toString() {
		return "IdDataPair [id=" + id + ", obj=" + obj + "]";
	} 
}
