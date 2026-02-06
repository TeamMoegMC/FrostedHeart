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

package com.teammoeg.frostedheart.content.town.house;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.content.town.worker.WorkerState;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

public class HouseState extends WorkerState {
	@Getter
	double temperatureRating,decorationRating,spaceRating;
	@Getter
	List<BlockPos> beds=new ArrayList<>();
	public HouseState() {
		
	}
	@Override
	public void writeNBT(CompoundTag tag, boolean isNetwork) {
		super.writeNBT(tag, isNetwork);
		
        tag.putDouble("temperatureRating",temperatureRating);
        tag.putDouble("decorationRating",decorationRating);
        tag.putDouble("spaceRating",spaceRating);
        tag.put("beds", SerializeUtil.toNBTList(beds,(o,b)->b.addLong(o.asLong())));
	}

	@Override
	public void readNBT(CompoundTag tag, boolean isNetwork) {
		super.readNBT(tag, isNetwork);
		
        temperatureRating = tag.getDouble("temperatureRating");
        decorationRating = tag.getDouble("decorationRating");
        spaceRating = tag.getDouble("spaceRating");
        beds.clear();
        ListTag bedstag=tag.getList("beds", Tag.TAG_LONG);
        for(Tag bedtag:bedstag) {
        	beds.add(BlockPos.of(((LongTag)bedtag).getAsLong()));
        }
	}

}
