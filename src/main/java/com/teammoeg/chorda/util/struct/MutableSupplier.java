package com.teammoeg.chorda.util.struct;

import java.util.function.Supplier;

public class MutableSupplier<T> implements Supplier<T> {
	T obj;
	public MutableSupplier() {
	}
	@Override
	public T get() {
		return obj;
	}
	public void set(T obj) {
		this.obj=obj;
	}

}
