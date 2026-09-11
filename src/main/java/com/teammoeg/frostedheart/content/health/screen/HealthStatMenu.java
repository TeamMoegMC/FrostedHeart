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

package com.teammoeg.frostedheart.content.health.screen;

import com.teammoeg.chorda.menu.CBaseMenu;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.util.LazyOptional;

public class HealthStatMenu extends CBaseMenu {
	public CDataSlot<Float> fat=CCustomMenuSlot.SLOT_PERCENTAGE.create(this);
	public CDataSlot<Float> protein=CCustomMenuSlot.SLOT_PERCENTAGE.create(this);
	public CDataSlot<Float> carbohydrate=CCustomMenuSlot.SLOT_PERCENTAGE.create(this);
	public CDataSlot<Float> vegetable=CCustomMenuSlot.SLOT_PERCENTAGE.create(this);

	public CDataSlot<Float> headTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> bodyTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> handsTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> legsTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> feetTemperature=CCustomMenuSlot.SLOT_FIXED.create(this);



	public HealthStatMenu(int pContainerId, Inventory inventoryPlayer, FriendlyByteBuf extraData) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
	}
	public HealthStatMenu(int pContainerId, Inventory inventoryPlayer) {
		super(FHMenuTypes.NUTRITION_GUI.get(), pContainerId,inventoryPlayer.player, 0);
		
		LazyOptional<NutritionCapability> nut_lo=NutritionCapability.getCapability(inventoryPlayer.player);
		nut_lo.ifPresent(cap->{
				fat.bind(()->cap.get().fat()/100);
				protein.bind(()->cap.get().protein()/100);
				carbohydrate.bind(()->cap.get().carbohydrate()/100);
				vegetable.bind(()->cap.get().vegetable()/100);
		});

		LazyOptional<PlayerTemperatureData> temp_lo = PlayerTemperatureData.getCapability(inventoryPlayer.player);
		temp_lo.ifPresent(data->{
			headTemperature.bind(()->data.getBodyTempByPart(BodyPart.HEAD));
			bodyTemperature.bind(()->data.getBodyTempByPart(BodyPart.TORSO));
			handsTemperature.bind(()->data.getBodyTempByPart(BodyPart.HANDS));
			legsTemperature.bind(()->data.getBodyTempByPart(BodyPart.LEGS));
			feetTemperature.bind(()->data.getBodyTempByPart(BodyPart.FEET));
		});
	}

}
