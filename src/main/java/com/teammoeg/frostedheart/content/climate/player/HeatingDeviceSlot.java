package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType.SlotKey;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.world.entity.EquipmentSlot;
import top.theillusivec4.curios.common.slottype.SlotType;

public class HeatingDeviceSlot {
	SlotType curios;
	SlotKey vanilla;
	BodyPart part;
	/**
	 * Heating device on body
	 * */
	public HeatingDeviceSlot(EquipmentSlot slot) {
		part=BodyPart.fromVanilla(slot);
		vanilla=EquipmentSlotType.fromVanilla(slot);
	}
	/**
	 * Heating device on curios
	 * */
	public HeatingDeviceSlot(SlotType curios) {
		super();
		this.curios = curios;
	}
	public boolean is(EquipmentSlot slot) {
		return vanilla!=null&&vanilla.is(slot);
	}
	public boolean is(BodyPart part) {
		return this.part==part;
	}
	public boolean is(String id) {
		return id.equals(this.curios.getIdentifier());
	}
}
