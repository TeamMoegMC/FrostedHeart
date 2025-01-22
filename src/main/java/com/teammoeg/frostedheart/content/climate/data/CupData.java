/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.io.CodecUtil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public record CupData(Item item,float efficiency){
	public static final MapCodec<CupData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		CodecUtil.registryCodec(()->BuiltInRegistries.ITEM).fieldOf("item").forGetter(o->o.item),
		Codec.FLOAT.fieldOf("efficiency").forGetter(o->o.efficiency)).apply(t, CupData::new));
    public Float getEfficiency() {
        return efficiency;
    }
}
