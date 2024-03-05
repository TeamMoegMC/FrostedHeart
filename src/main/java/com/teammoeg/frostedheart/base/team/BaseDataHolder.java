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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

import java.util.Optional;

import net.minecraft.nbt.CompoundNBT;

public class BaseDataHolder<T extends BaseDataHolder<T>> implements SpecialDataHolder<T>, NBTSerializable {

	public BaseDataHolder() {
	}

	@Override
	public void save(CompoundNBT nbt, boolean isPacket) {
        for(Entry<String, NBTSerializable> ent:data.entrySet()) {
        	nbt.put(ent.getKey(), ent.getValue().serialize(isPacket));
        }
	}

	@Override
	public void load(CompoundNBT data, boolean isPacket) {
		for(SpecialDataType<?,?> tc:SpecialDataTypes.TYPE_REGISTRY) {
        	if(data.contains(tc.getId())) {
        		getDataRaw(tc).deserialize(data.getCompound(tc.getId()),isPacket);
        	}
        }
	}
	
	Map<String,NBTSerializable> data=new HashMap<>();
	@SuppressWarnings("unchecked")
	public <U extends NBTSerializable> U getData(SpecialDataType<U,T> cap){
		return (U) data.computeIfAbsent(cap.getId(),s->cap.create((T) this));
	}
	public NBTSerializable getDataRaw(SpecialDataType<?,?> cap){
		return data.computeIfAbsent(cap.getId(),s->cap.createRaw(this));
	}

	@Override
	public <U extends NBTSerializable> Optional<U> getOptional(SpecialDataType<U, T> cap) {
		
		return Optional.ofNullable((U)data.get(cap.getId()));
	}
}
