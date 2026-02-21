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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedArea;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.teammoeg.frostedheart.content.town.Town.DEBUG_MODE;
import static java.lang.Double.NEGATIVE_INFINITY;

public class MineBaseBuilding extends AbstractTownResidentWorkBuilding {
	public static final Codec<MineBaseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
					Codec.BOOL.fieldOf("isStructureValid").forGetter(o -> o.isStructureValid),
					OccupiedArea.CODEC.fieldOf("occupiedArea").forGetter(o -> o.occupiedArea),
					Codec.INT.fieldOf("area").forGetter(o -> o.area),
					Codec.INT.fieldOf("volume").forGetter(o -> o.volume),
					CodecUtil.list(BlockPos.CODEC, ArrayList::new).fieldOf("linkedMines").forGetter(o -> new ArrayList<BlockPos>(o.linkedMines)),
					Codec.INT.fieldOf("maxResidents").forGetter(o -> o.maxResidents),
					Codec.DOUBLE.fieldOf("rating").forGetter(o -> o.rating),
					Codec.DOUBLE.fieldOf("temperature").forGetter(o -> o.temperature))
			.apply(t, MineBaseBuilding::new));

	public int area;

	public int volume;

	public Set<BlockPos> linkedMines;

	public double rating;//似乎暂时无用

	public double temperature;//矿井内部温度

	public MineBaseBuilding(BlockPos pos) {
		super(pos);
	}

	@Override
	public boolean isBuildingWorkable() {
		return super.isBuildingWorkable()
				&& isTemperatureValid();
	}

	public static boolean isTemperatureValid(double temperature){
		if (DEBUG_MODE) return true;
		return temperature > -10 && temperature < 40;
	}

	public boolean isTemperatureValid() {
		return isTemperatureValid(this.temperature);
	}

	/**
	 * Full constructor matching the CODEC definition for serialization/deserialization.
	 * 
	 * @param pos the block position
	 * @param isStructureValid whether the structure is valid
	 * @param occupiedArea the occupied area
	 * @param area the area
	 * @param volume the volume
	 * @param linkedMines list of linked mine positions
	 * @param maxResidents the maximum residents
	 * @param rating the building rating
	 * @param temperature the mine internal temperature
	 */
	public MineBaseBuilding(BlockPos pos, boolean isStructureValid, OccupiedArea occupiedArea, int area, int volume, ArrayList<BlockPos> linkedMines, int maxResidents, double rating, double temperature) {
		super(pos);
		this.isStructureValid = isStructureValid;
		this.occupiedArea = occupiedArea;
		this.area = area;
		this.volume = volume;
		this.linkedMines = new java.util.HashSet<>(linkedMines);
		this.maxResidents = maxResidents;
		this.rating = rating;
		this.temperature = temperature;
	}

	@Override
	public boolean work(Town town) {
		if(linkedMines == null||linkedMines.isEmpty())return false;
		if (town instanceof TeamTown teamTown) {
			double toModify=0;
			for(UUID residentID : residentsID) {
				Resident resident=teamTown.getResident(residentID).orElse(null);
				if(resident==null)continue;
				double efficiency=0.2 * getResidentScore(resident);
				if(efficiency<=0)continue;
				toModify+=efficiency;
			}
			final double picked=teamTown.maypickTerrainResource(TerrainResourceType.ORE, toModify);
			IActionExecutorHandler resourceExecutorHandler=teamTown.getActionExecutorHandler();
			Map<Item, Double> comprehensiveWeights = new HashMap<>();
			AtomicInteger validMines = new AtomicInteger();
			linkedMines.stream()
					.map(teamTown::getTownBuilding)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.filter(MineBuilding.class::isInstance)
					.map(building -> (MineBuilding) building)
					.filter(AbstractTownBuilding::isBuildingWorkable)
					.map(mine -> mine.biomePath)
					.map(MineBuilding::getWeights)
					.map(weights ->{
						//得到单个矿场归一化后的Weights
						double totalWeight=weights.values().stream().reduce(0, Integer::sum);
						Map<Item, Double> weightsNormalized=new HashMap<>();
						weights.forEach((key, value) -> weightsNormalized.put(key,  ((double)value / totalWeight)));
						validMines.addAndGet(1);
						return weightsNormalized;
					})
					.forEach(weights -> weights.forEach((key, value) -> comprehensiveWeights.merge(key, value, Double::sum)));
			//各个矿场权重求和后除以矿场数，再次归一化
			comprehensiveWeights.entrySet().forEach(entry -> entry.setValue(entry.getValue() / validMines.doubleValue()));
			double modified = comprehensiveWeights.entrySet().stream().map(entry -> new TownResourceActions.ItemResourceAction
							(new ItemStack(entry.getKey()), ResourceActionType.ADD, picked * entry.getValue(), ResourceActionMode.ATTEMPT))
					.map(resourceExecutorHandler::execute)
							.filter(result -> result instanceof TownResourceActionResults.ItemResourceActionResult)
									.map(result -> (TownResourceActionResults.ItemResourceActionResult) result)
											.filter(TownResourceActionResults.ItemResourceActionResult::allModified)
					.mapToDouble(TownResourceActionResults.ItemResourceActionResult::modifiedAmount)
					.sum();
			teamTown.pickTerrainResource(TerrainResourceType.ORE, modified);

			return true;
		}
		throw new IllegalArgumentException("MineBaseBuilding ERROR: Can't work in non-team town :" + town);
	}

	@Override
	public double getResidentPriority() {
		if(!this.isBuildingWorkable()) return NEGATIVE_INFINITY;
		int currentResidentNum = this.residentsID.size();
		if(currentResidentNum > maxResidents) return NEGATIVE_INFINITY;
		//double rating = state.getRating();
		return -currentResidentNum + 1.0 * currentResidentNum / maxResidents + 0.4/*the base priority of workerType*/;
	}

	@Override
	public double getResidentScore(Resident resident) {
		double healthPart = TownMathFunctions.CalculatingFunction2(resident.getHealth(), 0.12);
		double mentalPart = 0.6 + 0.4 * (0.524+0.5*(1-Math.exp(-0.03*resident.getMental())));
		double strengthPart = 0.3 + 0.7 * TownMathFunctions.CalculatingFunction1(resident.getStrength());
		double proficiencyPart = 0.6 + 0.4 * TownMathFunctions.CalculatingFunction1(resident.getWorkProficiency(MineBaseBuilding.class));
		return healthPart * mentalPart * strengthPart * proficiencyPart;
	}
}
