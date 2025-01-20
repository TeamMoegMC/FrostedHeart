package com.teammoeg.chorda.util.io;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

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
		return RecordCodecBuilder.mapCodec(t->t.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(IdDataPair::getId),
			original.forGetter(IdDataPair::getObj)).apply(t, IdDataPair::new));

	}
	@Override
	public String toString() {
		return "IdDataPair [id=" + id + ", obj=" + obj + "]";
	} 
}
