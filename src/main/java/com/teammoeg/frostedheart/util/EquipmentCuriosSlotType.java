package com.teammoeg.frostedheart.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.teammoeg.frostedheart.FHMain;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import top.theillusivec4.curios.api.SlotTypePreset;

public enum EquipmentCuriosSlotType {
	   MAINHAND(false),
	   OFFHAND(false),
	   FEET(false),
	   LEGS(false),
	   CHEST(false),
	   HEAD(false),
	   QUICKBAR(false,9),
	   INVENTORY(false,27),
	   CURIOS_HEAD(true),
	   CURIOS_NECKLACE(true),
	   CURIOS_BACK(true),
	   CURIOS_BODY(true), 
	   CURIOS_BRACELET(true),
	   CURIOS_HANDS(true), 
	   CURIOS_RING(true,2),
	   CURIOS_BELT(true),
	   CURIOS_CHARM(true),
	   CURIOS_CURIO(true),
	   CURIOS_GENERIC(true),
	   UNKNOWN(false);
	private final boolean isCurios;
	private final UUID[] slotUUID;
	private final String[] key;
	private EquipmentCuriosSlotType(boolean isCurios) {
		this.isCurios=isCurios;
		slotUUID=new UUID[1];
		key=new String[1];
		this.key[0]		 = FHMain.rl(this.name().toLowerCase()).toString();
		this.slotUUID[0] = UUID.nameUUIDFromBytes(key[0].getBytes(StandardCharsets.ISO_8859_1));
	}
	private EquipmentCuriosSlotType(boolean isCurios,int num) {
		this.isCurios = isCurios;
		slotUUID=new UUID[num];
		key=new String[num];
		int ix=MathHelper.ceil(Math.log10(num));
		String format="%0"+ix+"d";
		for(int i=0;i<num;i++) {
			this.key[i]		 = FHMain.rl(this.name().toLowerCase()+"_"+String.format(format, i)).toString();
			this.slotUUID[i] = UUID.nameUUIDFromBytes(key[i].getBytes(StandardCharsets.ISO_8859_1));
		}
	}
	public boolean isHand() {
		return this==MAINHAND||this==OFFHAND;
	}
	public static EquipmentCuriosSlotType fromVanilla(EquipmentSlotType vanilla) {
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
	public static EquipmentCuriosSlotType fromCurios(String id) {
		SlotTypePreset stp=SlotTypePreset.findPreset(id).orElse(null);
		if(stp==null)return CURIOS_GENERIC;
		String curiosType="CURIOS_"+stp.name();
		EquipmentCuriosSlotType est=EquipmentCuriosSlotType.valueOf(curiosType);
		if(est!=null)
			return est;
		return CURIOS_GENERIC;
	}
	public boolean isCurios() {
		return isCurios;
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
