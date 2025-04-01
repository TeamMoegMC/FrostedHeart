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

package com.teammoeg.frostedheart.content.climate.block;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.slots.ArmorSlot;
import com.teammoeg.chorda.menu.slots.OffHandSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.player.BodyPartData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;

public class ClothesInventoryMenu extends CBaseMenu {
	Map<BodyPart,Pair<CDataSlot<Float>,CDataSlot<Float>>> partInsulation=new EnumMap<>(BodyPart.class);
	{
		for(BodyPart bp:BodyPart.values())
			partInsulation.put(bp, Pair.of(CCustomMenuSlot.SLOT_FIXED.create(this), CCustomMenuSlot.SLOT_FIXED.create(this)));
	}
	public ClothesInventoryMenu(int id, Inventory inventoryPlayer, FriendlyByteBuf extraData) {
		this(id, inventoryPlayer);
	}

	public ClothesInventoryMenu(int id, Inventory inventoryPlayer) {
		super(FHMenuTypes.CLOTHES_GUI.get(), id, inventoryPlayer.player, 14);
		PlayerTemperatureData ptd = PlayerTemperatureData.getCapability(inventoryPlayer.player).resolve().get();
		createLiningSlots(inventoryPlayer,ptd);
		for(BodyPart bp:BodyPart.values()) {
			partInsulation.get(bp).getFirst().bind(()->ptd.getThermalConductivityByPart(inventoryPlayer.player,bp));
			partInsulation.get(bp).getSecond().bind(()->ptd.getFluidResistanceByPart(inventoryPlayer.player,bp));
		}
		super.addPlayerInventory(inventoryPlayer, 8, 120, 178);
	}

	private void createLiningSlots(Inventory inventoryPlayer,PlayerTemperatureData ptd) {
		
		int y0 = 6;
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.HEAD, inventoryPlayer, 39, 6, y0));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.CHEST, inventoryPlayer, 38, 6, y0 + 18));
		this.addSlot(new OffHandSlot(inventoryPlayer.player, inventoryPlayer, 40, 6, y0 + 18 * 2));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.LEGS, inventoryPlayer, 37, 6, y0 + 18 * 3));
		this.addSlot(new ArmorSlot(inventoryPlayer.player, EquipmentSlot.FEET, inventoryPlayer, 36, 6, y0 + 18 * 4));
		for (int j = 0; j < 5; j++) {
			BodyPart bp=BodyPart.values()[j];
			BodyPartData clothes=ptd.clothesOfParts.get(bp);
			for (int k = 0; k < clothes.getSize(); ++k) {
				this.addSlot(new LiningSlot(inventoryPlayer.player,bp,
						clothes.clothes, k, 118 + k * 18, 6+18*j));
			}
		}
	}

}
