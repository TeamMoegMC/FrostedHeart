package com.teammoeg.frostedheart.team;

import java.util.Objects;
import java.util.function.Function;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

public class SpecialDataType<T extends NBTSerializable,U extends SpecialDataHolder<U>>{
	String id;
	Function<U,T> factory;
	
	public SpecialDataType(String id, Function<U, T> factory) {
		super();
		this.id = id;
		this.factory = factory;
		SpecialDataTypes.TYPE_REGISTRY.add(this);
	}
	public T create(U dat) {
		return factory.apply(dat);
	}
	public NBTSerializable createRaw(SpecialDataHolder dat) {
		return factory.apply((U) dat);
	}
	public T getOrCreate(SpecialDataHolder<U> dat) {
		return dat.getData(this);
	}
	public String getId() {
		return id;
	}
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		SpecialDataType<?,?> other = (SpecialDataType<?,?>) obj;
		return Objects.equals(id, other.id);
	};
}
