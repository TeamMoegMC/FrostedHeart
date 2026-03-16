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

package com.teammoeg.chorda.io;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * 将资源位置ID与数据对象配对的通用容器，支持Codec序列化。
 * <p>
 * A generic container that pairs a ResourceLocation ID with a data object, supporting Codec serialization.
 *
 * @param <T> 数据对象类型 / the type of the data object
 */
public class IdDataPair<T> {
	ResourceLocation id;
	T obj;
	/**
	 * 构造一个新的ID-数据对。
	 * <p>
	 * Constructs a new ID-data pair.
	 *
	 * @param id 资源位置ID / the resource location ID
	 * @param obj 数据对象 / the data object
	 */
	public IdDataPair(ResourceLocation id, T obj) {
		super();
		this.id = id;
		this.obj = obj;
	}
	/**
	 * 获取资源位置ID。
	 * <p>
	 * Gets the resource location ID.
	 *
	 * @return 资源位置 / the resource location
	 */
	public ResourceLocation getId() {
		return id;
	}
	/**
	 * 获取数据对象。
	 * <p>
	 * Gets the data object.
	 *
	 * @return 数据对象 / the data object
	 */
	public T getObj() {
		return obj;
	}

	/**
	 * 为IdDataPair创建一个MapCodec，将ID和数据对象合并编解码。
	 * <p>
	 * Creates a MapCodec for IdDataPair that combines ID and data object encoding/decoding.
	 *
	 * @param <A> 数据对象类型 / the data object type
	 * @param original 原始数据对象的MapCodec / the original data object MapCodec
	 * @return IdDataPair的MapCodec / the MapCodec for IdDataPair
	 */
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
