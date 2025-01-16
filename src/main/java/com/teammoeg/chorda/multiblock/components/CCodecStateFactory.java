package com.teammoeg.chorda.multiblock.components;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

public class CCodecStateFactory<T> {
	final Codec<T> Save;
	final Codec<T> Sync;
	final Supplier<T> factory;
	public CCodecStateFactory(Codec<T> save, Codec<T> sync, Supplier<T> factory) {
		super();
		Save = save;
		Sync = sync;
		this.factory = factory;
	}

}
