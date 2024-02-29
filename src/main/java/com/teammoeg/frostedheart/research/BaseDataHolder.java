package com.teammoeg.frostedheart.research;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.teammoeg.frostedheart.util.NBTSerializable;

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
}
