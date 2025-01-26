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

package com.teammoeg.chorda.dataholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.io.SerializeUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class DataHolderMap<T extends DataHolderMap<T>> implements SpecialDataHolder<T>, NBTSerializable {

	public final Marker marker;
	Map<SpecialDataType,SpecialData> data=new ConcurrentHashMap<>();
	
	public DataHolderMap(String markerName) {
		marker = MarkerManager.getMarker(markerName);
	}

	@Override
	public void save(CompoundTag nbt, boolean isPacket) {
		nbt.put("data", SerializeUtil.toNBTMap(data.entrySet(), (t,p)-> {
			try {
				p.put(t.getKey().getId(),(Tag)t.getKey().saveData(NbtOps.INSTANCE, t.getValue()));
			} catch (Exception e) {
				Chorda.LOGGER.error(marker, "Failed to save " + t.getKey(), e);
			}
		}));
	}

	@Override
	public void load(CompoundTag data, boolean isPacket) {
		data = data.getCompound("data");
		for(SpecialDataType<?> tc: SpecialDataType.TYPE_REGISTRY) {
        	if(data.contains(tc.getId())) {
				try {
					SpecialData raw=tc.loadData(NbtOps.INSTANCE, data.get(tc.getId()));
					this.data.put(tc, raw);
				} catch (Exception e) {
					Chorda.LOGGER.error(marker, "Failed to load " + tc, e);
				}
        	}
        }
	}
	
	
	@SuppressWarnings("unchecked")
	public <U extends SpecialData> U getData(SpecialDataType<U> cap){
		U ret= (U) data.computeIfAbsent(cap,s->cap.create((T) this));
		return ret;
	}
	public <U extends SpecialData> U setData(SpecialDataType<U> cap, U data){
		this.data.put(cap, data);
		return data;
	}
	public SpecialData getDataRaw(SpecialDataType<?> cap){
		SpecialData ret= data.computeIfAbsent(cap,s->cap.createRaw(this));
		return ret;
	}

	@Override
	public <U extends SpecialData> Optional<U> getOptional(SpecialDataType<U> cap) {
		
		return Optional.ofNullable((U)data.get(cap));
	}
}
