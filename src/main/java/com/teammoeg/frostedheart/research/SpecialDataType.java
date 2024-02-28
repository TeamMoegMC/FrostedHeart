package com.teammoeg.frostedheart.research;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.NBTSerializable;

public class SpecialDataType<T extends NBTSerializable>{
	public static final Set<SpecialDataType<?>> TYPE_REGISTRY=new HashSet<>();
	String id;
	Function<SpecialDataHolder,T> factory;
	
	public SpecialDataType(String id, Function<SpecialDataHolder, T> factory) {
		super();
		this.id = id;
		this.factory = factory;
		TYPE_REGISTRY.add(this);
	}
	public T create(SpecialDataHolder dat) {
		return factory.apply(dat);
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
		SpecialDataType<?> other = (SpecialDataType<?>) obj;
		return Objects.equals(id, other.id);
	};
}
