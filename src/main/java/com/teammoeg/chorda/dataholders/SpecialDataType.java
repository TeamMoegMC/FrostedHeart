/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.chorda.dataholders;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * Type of special data
 *
 * @param <T> the data component data type
 * <U>: the data holder actual type
 */
@ToString
public class SpecialDataType<T extends SpecialData>{
	static final Set<SpecialDataType<?>> TYPE_REGISTRY=new HashSet<>();
	@Getter
	private String id;
	@ToString.Exclude
	private Function<SpecialDataHolder,T> factory;
	@Getter
	private Codec<T> codec;
	
	final int numId;
	private static int nid=0;
	public static SpecialDataType<?> getType(String id){
		for(SpecialDataType<?> data:TYPE_REGISTRY) {
			if(data.getId().equals(id))
				return data;
		}
		return null;
	}
	/**
	 * Instantiates and register a new special data type.
	 *
	 * @param id the data id
	 * @param factory the data object factory, current team would be provided to initiate the data object
	 * @param codec codec for serialization and networking
	 */
	public  SpecialDataType(String id, Function<SpecialDataHolder, T> factory, Codec<T> codec) {
		super();
		this.id = id;
		this.factory = factory;
		this.codec=codec;
		synchronized(TYPE_REGISTRY) {
			TYPE_REGISTRY.add(this);
			numId=nid++;
		}
	}
	
	/**
	 * Creates a data component
	 *
	 * @param data the data holder
	 * @return created data component
	 */
	public <U extends SpecialDataHolder> T create(U data) {
		return factory.apply(data);
	}
	
	/**
	 * Create data component with raw type and no type check.
	 *
	 * @param data the data holder
	 * @return created data component
	 */
	public <T extends SpecialDataHolder> SpecialData createRaw(T data) {
		return factory.apply(data);
	}
	public <U> T loadData(DynamicOps<U> ops,U data) throws Exception {
		try {
			return CodecUtil.decodeOrThrow(codec.decode(ops, data));
		} catch (Exception e) {
			Chorda.LOGGER.error("Error loading data for SpecialDataType " + this);
			Chorda.LOGGER.error("Data: " + data);
			e.printStackTrace();
			// throw
			throw new Exception("Error loading data for SpecialDataType " + this, e);
		}
    }
	public <U> U saveData(DynamicOps<U> ops,T data) throws Exception {
		try {
			return CodecUtil.encodeOrThrow(codec.encodeStart(ops, data));
		} catch (Exception e) {
			Chorda.LOGGER.error("Error saving data for SpecialDataType " + this);
			Chorda.LOGGER.error("Data: " + data);
			e.printStackTrace();
			// throw
			throw new Exception("Error saving data for SpecialDataType " + this, e);
		}

	}
	
	/**
	 * Get or create data
	 *
	 * @param data the data holder
	 * @return data component
	 */
	public <U extends SpecialDataHolder<U>> T getOrCreate(U data) {
		if(data==null)return null;
		return data.getData(this);
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
	}
}
