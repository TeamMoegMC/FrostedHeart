package com.teammoeg.frostedheart.team;

import java.util.Objects;
import java.util.function.Function;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

/**
 * Type of special data
 *
 * @param <T> the data component data type
 * @param <U> the data holder actual type
 */
public class SpecialDataType<T extends NBTSerializable,U extends SpecialDataHolder<U>>{
	
	private String id;
	private Function<U,T> factory;
	
	/**
	 * Instantiates and register a new special data type.
	 *
	 * @param id the id
	 * @param factory the factory
	 */
	public SpecialDataType(String id, Function<U, T> factory) {
		super();
		this.id = id;
		this.factory = factory;
		SpecialDataTypes.TYPE_REGISTRY.add(this);
	}
	
	/**
	 * Creates a data component
	 *
	 * @param data the data holder
	 * @return created data component
	 */
	public T create(U data) {
		return factory.apply(data);
	}
	
	/**
	 * Create data component with raw type and no type check.
	 *
	 * @param data the data holder
	 * @return created data component
	 */
	public NBTSerializable createRaw(SpecialDataHolder data) {
		return factory.apply((U) data);
	}
	
	/**
	 * Get or create data
	 *
	 * @param data the data holder
	 * @return data component
	 */
	public T getOrCreate(SpecialDataHolder<U> data) {
		return data.getData(this);
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
