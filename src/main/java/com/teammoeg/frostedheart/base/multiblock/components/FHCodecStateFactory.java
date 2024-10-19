package com.teammoeg.frostedheart.base.multiblock.components;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

public class FHCodecStateFactory<T> {
	final Codec<T> Save;
	final Codec<T> Sync;
	final Supplier<T> factory;
	public FHCodecStateFactory(Codec<T> save, Codec<T> sync, Supplier<T> factory) {
		super();
		Save = save;
		Sync = sync;
		this.factory = factory;
	}

}
