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
