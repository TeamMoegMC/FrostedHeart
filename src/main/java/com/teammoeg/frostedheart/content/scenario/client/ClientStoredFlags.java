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

package com.teammoeg.frostedheart.content.scenario.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.io.CodecUtil;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

public class ClientStoredFlags implements SpecialData{
	public static final Codec<ClientStoredFlags> CODEC=RecordCodecBuilder.create(t->
	t.group(CompoundTag.CODEC.fieldOf("flags").forGetter(o->o.data))
			.apply(t, ClientStoredFlags::new));
	@Getter
	CompoundTag data;

	public ClientStoredFlags(CompoundTag data) {
		super();
		this.data = data;
	}
	public ClientStoredFlags(SpecialDataHolder holder) {
		super();
		this.data = new CompoundTag();
	}
}
