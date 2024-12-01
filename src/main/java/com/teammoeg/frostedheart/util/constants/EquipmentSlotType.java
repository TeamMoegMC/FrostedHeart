package com.teammoeg.frostedheart.util.constants;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.Mth;
import top.theillusivec4.curios.api.SlotTypePreset;
/**
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
	private final UUID[] slotUUID;
	private final String[] key;
	private EquipmentSlotType() {
		slotUUID=new UUID[1];
		key=new String[1];
		this.key[0]		 = FHMain.rl(this.name().toLowerCase()).toString();
		this.slotUUID[0] = UUID.nameUUIDFromBytes(key[0].getBytes(StandardCharsets.ISO_8859_1));
	}
	private EquipmentSlotType(int maxslot) {
		slotUUID=new UUID[maxslot];
		key=new String[maxslot];
		int ix=Mth.ceil(Math.log10(maxslot));
		String format="%0"+ix+"d";
		for(int i=0;i<maxslot;i++) {
			this.key[i]		 = FHMain.rl(this.name().toLowerCase()+"_"+String.format(format, i)).toString();
			this.slotUUID[i] = UUID.nameUUIDFromBytes(key[i].getBytes(StandardCharsets.ISO_8859_1));
		}
	}
	public boolean isHand() {
		return this==MAINHAND||this==OFFHAND;
	}
	public static EquipmentSlotType fromVanilla(EquipmentSlot vanilla) {
		switch(vanilla) {
		case MAINHAND: return MAINHAND;
		case OFFHAND: return OFFHAND;
		case FEET: return FEET;
		case LEGS: return LEGS;
		case CHEST: return CHEST;
		case HEAD: return HEAD;
		}
		return UNKNOWN;
	}
	public UUID getSlotUUID(int id) {
		return slotUUID[id];
	}
	public String getKey(int id) {
		return key[id];
	}
	public int size() {
		return key.length;
	}
	
}
