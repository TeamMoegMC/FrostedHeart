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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;

import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.teammoeg.frostedheart.content.town.Town.DEBUG_MODE;
import static java.lang.Double.NEGATIVE_INFINITY;

public class HuntingBaseBuilding extends AbstractTownResidentWorkBuilding {
	public static final Codec<HuntingBaseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
					Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
					OccupiedArea.CODEC.fieldOf("occupiedArea").forGetter(o -> o.occupiedArea),
					Codec.INT.fieldOf("area").forGetter(o -> o.area),
					Codec.INT.fieldOf("volume").forGetter(o -> o.volume),
					Codec.DOUBLE.fieldOf("temperature").forGetter(o -> o.temperature),
					Codec.INT.fieldOf("maxResidents").forGetter(o -> o.maxResidents),
					Codec.INT.fieldOf("tanningRackNum").forGetter(o -> o.tanningRackNum),
					Codec.DOUBLE.fieldOf("temperatureModifier").forGetter(o -> o.temperatureModifier))
			.apply(t, HuntingBaseBuilding::new));
	// get max resident
	@Getter
	public int maxResidents;

	public boolean isStructureValid;

	public int area;

	public int volume;

	public int tanningRackNum;

	public double temperature;

	public double temperatureModifier;

	public double rating;

	public HuntingBaseBuilding(BlockPos pos) {
        super(pos);
    }

	/**
	 * Full constructor matching the CODEC definition for serialization/deserialization.
	 * 
	 * @param pos the block position
	 * @param isStructureValid whether the structure is valid
	 * @param occupiedArea the occupied area
	 * @param area the area
	 * @param volume the volume
	 * @param temperature the temperature
	 * @param maxResidents the maximum residents
	 * @param tanningRackNum the number of tanning racks
	 * @param temperatureModifier the temperature modifier
	 */
	public HuntingBaseBuilding(BlockPos pos, boolean isStructureValid, OccupiedArea occupiedArea, int area, int volume, double temperature, int maxResidents, int tanningRackNum, double temperatureModifier) {
		super(pos);
		this.isStructureValid = isStructureValid;
		this.occupiedArea = occupiedArea;
		this.area = area;
		this.volume = volume;
		this.temperature = temperature;
		this.maxResidents = maxResidents;
		this.tanningRackNum = tanningRackNum;
		this.temperatureModifier = temperatureModifier;
	}

	@Override
	public boolean work(Town town) {
		if (town instanceof TeamTown teamTown) {
			double totalEfficiency=0;
			for(Resident resident:this.getResidents(teamTown)) {
				if(resident==null)continue;
				double efficiency=0.2 * getResidentScore(resident);
				if(efficiency<=0)continue;
				totalEfficiency+=efficiency;
			}
			double picked=teamTown.maypickTerrainResource(TerrainResourceType.HUNT, totalEfficiency*2);

			TownResourceActionResults.ItemResourceActionResult result = (TownResourceActionResults.ItemResourceActionResult) town
					.getActionExecutorHandler()
					.execute(new TownResourceActions.ItemResourceAction(new ItemStack(Items.BEEF), ResourceActionType.ADD, picked, ResourceActionMode.MAXIMIZE));
			teamTown.pickTerrainResource(TerrainResourceType.HUNT, result.modifiedAmount());

			return true;
		}
		FHMain.LOGGER.error("HuntingBaseBuilding.work: Town is not TeamTown, need to fix work method.");//添加对其它城镇的适配
		throw new IllegalArgumentException("HuntingBaseBuilding ERROR: Can't work in non-team town :" + town);
	}

	@Override
	public boolean isBuildingWorkable() {
		return super.isBuildingWorkable()
				&& isTemperatureValid()
				&& isSpaceValid();
	}

	@Override
	public double getResidentPriority() {
		if(!this.isBuildingWorkable()) return NEGATIVE_INFINITY;
		int currentResidentNum = this.residentsID.size();
		if(currentResidentNum > maxResidents) return NEGATIVE_INFINITY;
		return -currentResidentNum + (double) currentResidentNum / maxResidents + 0.5/*the base priority of workerType*/ + rating;
	}

	@Override
	public double getResidentScore(Resident resident) {
		double healthPart =  1 / (1 + Math.exp(-resident.getHealth() * 0.09 + 5.5 )) + 0.028;
		double mentalPart = 0.2 + 0.8 * Math.sqrt(resident.getMental() / 100);
		double strengthPart = 0.3 + 0.7 * TownMathFunctions.CalculatingFunction1(resident.getStrength());
		double intelligencePart = 0.8 + 0.2 * TownMathFunctions.CalculatingFunction1(resident.getIntelligence());
		double workProficiencyPart = 0.1 + 0.9 * TownMathFunctions.CalculatingFunction1(resident.getWorkProficiency(HuntingBaseBuilding.class));
		return healthPart * mentalPart * strengthPart * intelligencePart * workProficiencyPart;
	}

	public double getEffectiveTemperature() {
		return temperature + temperatureModifier;
	}

	public static boolean isTemperatureValid(double effectiveTemperature){
		if (DEBUG_MODE) return true;
		return effectiveTemperature >= TownMathFunctions.COMFORTABLE_TEMP_HOUSE;
	}

	public boolean isTemperatureValid() {
		return isTemperatureValid(getEffectiveTemperature());
	}

	public boolean isSpaceValid(){
		return this.area >= 4 && this.volume >= 8;
	}
}
