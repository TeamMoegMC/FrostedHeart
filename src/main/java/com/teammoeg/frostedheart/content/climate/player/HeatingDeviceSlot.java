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

package com.teammoeg.frostedheart.content.climate.player;

import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType.SlotKey;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.world.entity.EquipmentSlot;
import top.theillusivec4.curios.api.type.ISlotType;

public class HeatingDeviceSlot {
	static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();
	private static final HeatingDeviceSlot[] VANILLA_SLOTS =
			createVanillaSlots();
	ISlotType curios;
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
	public HeatingDeviceSlot(ISlotType curios) {
		super();
		this.curios = curios;
	}

	HeatingDeviceSlot() {
	}

	static HeatingDeviceSlot vanilla(EquipmentSlot slot) {
		return VANILLA_SLOTS[slot.ordinal()];
	}

	void setCurios(ISlotType curios) {
		this.curios = curios;
		this.vanilla = null;
		this.part = null;
	}

	private static HeatingDeviceSlot[] createVanillaSlots() {
		HeatingDeviceSlot[] result = new HeatingDeviceSlot[EQUIPMENT_SLOTS.length];
		for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
			result[slot.ordinal()] = new HeatingDeviceSlot(slot);
		}
		return result;
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
	public boolean isHand() {
		return vanilla!=null&&vanilla.isHand();
	}
}
