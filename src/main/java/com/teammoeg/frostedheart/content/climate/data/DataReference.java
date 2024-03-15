package com.teammoeg.frostedheart.content.climate.data;

import com.mojang.serialization.MapCodec;
import com.teammoeg.frostedheart.util.io.JointCodec;

import net.minecraft.util.ResourceLocation;

public class DataReference<T> {
	ResourceLocation id;
	T obj;
	public DataReference(ResourceLocation id, T obj) {
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

	public static <A> MapCodec<DataReference<A>> createCodec(MapCodec<A> original){
		return new JointCodec<ResourceLocation,A,DataReference<A>>(ResourceLocation.CODEC.fieldOf("id"), original, DataReference::new, DataReference::getId, DataReference::getObj);
	}
	@Override
	public String toString() {
		return "DataReference [id=" + id + ", obj=" + obj + "]";
	} 
}
