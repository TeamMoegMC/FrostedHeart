/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.codec.CompressDifferCodec;

import net.minecraft.nbt.CompoundTag;

public class ClueData{
	public static final Codec<ClueData> FULL_CODEC=RecordCodecBuilder.create(t->t.group(
			Codec.BOOL.fieldOf("completed").forGetter(o->o.completed),
			CompoundTag.CODEC.fieldOf("data").forGetter(o->o.data)).apply(t, ClueData::new));
	public static final Codec<ClueData> CODEC=new CompressDifferCodec<>(RecordCodecBuilder.create(t->t.group(
			Codec.BOOL.fieldOf("completed").forGetter(o->o.completed)).apply(t, ClueData::new)), FULL_CODEC);
	boolean completed;
	CompoundTag data;
	
	public ClueData() {
		super();
	}

	public ClueData(boolean completed, CompoundTag data) {
		super();
		
		this.completed = completed;
		this.data = data;
	}
	
	public ClueData(boolean completed) {
		super();
		this.completed = completed;
	}

	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	public CompoundTag getData() {
		return data;
	}
	public void setData(CompoundTag data) {
		this.data = data;
	}

}
