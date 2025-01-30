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

package com.teammoeg.frostedheart.content.climate.player;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * A way to markup inventory and equipment slot universally
 * used to store uuids for frosted heart equipment attributes 
 * 
 * */
public enum EquipmentSlotType {
	   MAINHAND(),
	   OFFHAND(),
	   FEET(),
	   LEGS(),
	   CHEST(),
	   HEAD(),
	   QUICKBAR(9),
	   INVENTORY(27),
	   UNKNOWN();
	public static record SlotKey(EquipmentSlotType slotType,String slotName,UUID slotUUID,int slotNum){
		public boolean is(EquipmentSlotType slot) {
			return slot==slotType;
		}
		public boolean is(EquipmentSlotType slot,int slotNum) {
			return slot==slotType&&this.slotNum()==slotNum;
		}
		public boolean isHand() {
			switch(slotType) {
			case MAINHAND:
			case OFFHAND:return true;
			};
			return false;
		}
		public boolean isArmor() {
			switch(slotType) {
			case FEET:
			case LEGS:
			case CHEST:
			case HEAD:return true;
			};
			return false;
		}
		public AttributeModifier createAttribute(double amount,Operation operation) {
			return new AttributeModifier(slotUUID,this::slotName,amount,operation);
		}
	}
	private final SlotKey[] slots;
	public static final Map<String,SlotKey> lookup=Util.make(new HashMap<>(), m->{
		for(EquipmentSlotType sl:EquipmentSlotType.values())
			for(SlotKey key:sl.slots) {
				m.put(key.slotName, key);
			}
	});
	private EquipmentSlotType() {
		slots=new SlotKey[1];
		String slkey=this.name().toLowerCase();
		slots[0]=new SlotKey(this,slkey,UUID.nameUUIDFromBytes(FHMain.rl(slkey).toString().getBytes(StandardCharsets.ISO_8859_1)),0);
	}
	private EquipmentSlotType(int maxslot) {
		slots=new SlotKey[maxslot];
		int ix=Mth.ceil(Math.log10(maxslot));
		String format="%0"+ix+"d";
		for(int i=0;i<maxslot;i++) {
			String slkey=this.name().toLowerCase()+"_"+String.format(format, i);
			this.slots[i] = new SlotKey(this,slkey,UUID.nameUUIDFromBytes(FHMain.rl(slkey).toString().getBytes(StandardCharsets.ISO_8859_1)),i);
		}
	}
	public boolean isHand() {
		return this==MAINHAND||this==OFFHAND;
	}
	public static SlotKey fromVanilla(EquipmentSlot vanilla) {
		switch(vanilla) {
		case MAINHAND: return MAINHAND.getSlot();
		case OFFHAND: return OFFHAND.getSlot();
		case FEET: return FEET.getSlot();
		case LEGS: return LEGS.getSlot();
		case CHEST: return CHEST.getSlot();
		case HEAD: return HEAD.getSlot();
		}
		return UNKNOWN.getSlot();
	}
	public UUID getSlotUUID(int id) {
		return slots[id].slotUUID;
	}
	public String getKey(int id) {
		return slots[id].slotName;
	}
	public SlotKey getSlot(int id) {
		return slots[id];
	}
	public SlotKey getSlot() {
		return slots[0];
	}
	public int size() {
		return slots.length;
	}
	
}
