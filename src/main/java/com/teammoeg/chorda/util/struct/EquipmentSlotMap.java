package com.teammoeg.chorda.util.struct;

import java.util.EnumMap;

import net.minecraft.world.entity.EquipmentSlot;

public class EquipmentSlotMap<T> {
	private T defaultSlot;
	private EnumMap<EquipmentSlot,T> map=new EnumMap<>(EquipmentSlot.class);
	
	
	public void put(EquipmentSlot slot,T data) {
		if(slot==null)
			defaultSlot=data;
		else
			map.put(slot, data);
	};
	public T get(EquipmentSlot slot) {
		return map.getOrDefault(slot, defaultSlot);
	}
}
