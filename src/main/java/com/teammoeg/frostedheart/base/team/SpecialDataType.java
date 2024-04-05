/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.base.team;

import java.util.Objects;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import com.teammoeg.frostedheart.util.io.codec.DataOps;

/**
 * Type of special data
 *
 * @param <T> the data component data type
 * @param <U> the data holder actual type
 */
public class SpecialDataType<T extends SpecialData>{
	
	private String id;
	private Function<SpecialDataHolder,T> factory;
	private Codec<T> codec;
	
	/**
	 * Instantiates and register a new special data type.
	 *
	 * @param id the id
	 * @param factory the factory
	 */
	public  SpecialDataType(String id, Function<SpecialDataHolder, T> factory, Codec<T> codec) {
		super();
		this.id = id;
		this.factory = factory;
		this.codec=codec;
		SpecialDataTypes.TYPE_REGISTRY.add(this);
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
	public <U> T loadData(DynamicOps<U> ops,U data) {
//		System.out.println(this.getId());
//		System.out.println("=============data element===============");
//		System.out.println(data);
		return CodecUtil.decodeOrThrow(codec.decode(ops, data));
	}
	public <U> U saveData(DynamicOps<U> ops,T data) {
//		System.out.println(this.getId());
		return CodecUtil.encodeOrThrow(codec.encodeStart(ops, data));
	}
	
	/**
	 * Get or create data
	 *
	 * @param data the data holder
	 * @return data component
	 */
	public <U extends SpecialDataHolder<U>> T getOrCreate(U data) {
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
		SpecialDataType<?> other = (SpecialDataType<?>) obj;
		return Objects.equals(id, other.id);
	}
}
