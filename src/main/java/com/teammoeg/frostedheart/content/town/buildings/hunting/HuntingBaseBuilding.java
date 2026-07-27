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
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.UUIDUtil;

import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.*;

import static com.teammoeg.frostedheart.content.town.ITown.DEBUG_MODE;
import static java.lang.Double.NEGATIVE_INFINITY;

public class HuntingBaseBuilding extends AbstractTownResidentWorkBuilding {
	public static final Codec<HuntingBaseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
					BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
					Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
					OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.getOccupiedVolume()),
					Codec.list(UUIDUtil.CODEC).optionalFieldOf("residentsID",List.of()).forGetter(o -> new ArrayList<>(o.getResidentsID())),
					Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.getArea()),
					Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.getVolume()),
					Codec.DOUBLE.optionalFieldOf("temperature",0D).forGetter(o -> o.getTemperature()),
					Codec.INT.optionalFieldOf("maxResidents",0).forGetter(o -> o.getMaxResidents()),
					Codec.INT.optionalFieldOf("tanningRackNum",0).forGetter(o -> o.getTanningRackNum()),
					Codec.DOUBLE.optionalFieldOf("temperatureModifier",0D).forGetter(o -> o.getTemperatureModifier()),
					Codec.DOUBLE.optionalFieldOf("lootRollCarry",0D).forGetter(o -> o.getLootRollCarry()))
			.apply(t, HuntingBaseBuilding::new));
	@Getter
	private int area;
	@Getter
	private int volume;
	@Getter
	private int tanningRackNum;
	@Getter
	private double temperature;
	@Getter
	private double temperatureModifier;
	@Getter
	private double rating;
	@Getter
	private double lootRollCarry;

	public void setArea(int area) { this.area = area; fireChange(); }
	public void setVolume(int volume) { this.volume = volume; fireChange(); }
	public void setTanningRackNum(int tanningRackNum) { this.tanningRackNum = tanningRackNum; fireChange(); }
	public void setTemperature(double temperature) { this.temperature = temperature; fireChange(); }
	public void setTemperatureModifier(double temperatureModifier) { this.temperatureModifier = temperatureModifier; fireChange(); }
	public void setRating(double rating) { this.rating = rating; fireChange(); }
	public void setLootRollCarry(double lootRollCarry) {
		double safeCarry = Double.isFinite(lootRollCarry)
				? Math.max(0.0, Math.min(Math.nextDown(1.0), lootRollCarry))
				: 0.0;
		if (Double.compare(this.lootRollCarry, safeCarry) != 0) {
			this.lootRollCarry = safeCarry;
			fireChange();
		}
	}

	public HuntingBaseBuilding(BlockPos pos) {
        super(pos);
    }

	/**
	 * Full constructor matching the CODEC definition for serialization/deserialization.
	 * 
	 * @param pos the block position
	 * @param isStructureValid whether the structure is valid
	 * @param occupiedVolume the occupied area
	 * @param residentsID list of resident UUIDs (will be converted to Set)
	 * @param area the area
	 * @param volume the volume
	 * @param temperature the temperature
	 * @param maxResidents the maximum residents
	 * @param tanningRackNum the number of tanning racks
	 * @param temperatureModifier the temperature modifier
	 * @param lootRollCarry fractional expected loot-table rolls retained between work cycles
	 */
	public HuntingBaseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, java.util.List<UUID> residentsID, int area, int volume, double temperature, int maxResidents, int tanningRackNum, double temperatureModifier, double lootRollCarry) {
		super(pos);
		this.setIsStructureValid(isStructureValid);
		this.setOccupiedVolume(occupiedVolume);
		this.residentsID = new java.util.HashSet<>(residentsID);
		this.setArea(area);
		this.setVolume(volume);
		this.setTemperature(temperature);
		this.setMaxResidents(maxResidents);
		this.setTanningRackNum(tanningRackNum);
		this.setTemperatureModifier(temperatureModifier);
		this.setLootRollCarry(lootRollCarry);
	}

	@Override
	public boolean work(ITownWithBuildings town, ServerLevel world) {
        if (!(town instanceof TeamTown teamTown)) {
            FHMain.LOGGER.error("HuntingBaseBuilding.work: ITown is not TeamTown, need to fix work method.");//添加对其它城镇的适配
            throw new IllegalArgumentException("HuntingBaseBuilding ERROR: Can't work in non-team town :" + town);
        }

		// 1. Dimensionless productivity relative to a standard hunter.
		double totalProductivity = 0.0;
		Collection<Resident> residents = this.getResidents(teamTown);
		for (Resident resident : residents) {
			if (resident == null) continue;
			double productivity = getResidentScore(resident);
			if (productivity > 0) totalProductivity += productivity;
		}

		// 2. Settle expected rolls. Only the fractional remainder is carried to
		// the next town day; whole rolls blocked by terrain supply are not backlogged.
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		double expectedThrows = config.passiveExpectedLootRollsPerBaseDay.get()
				+ totalProductivity * config.expectedLootRollsPerStandardWorkerDay.get();
		int desiredThrows;
		if (config.useFractionalLootRollCarry.get()) {
			TownMathFunctions.FractionalSettlement settlement =
					TownMathFunctions.settleFractionalAmount(lootRollCarry, expectedThrows);
			desiredThrows = (int) Math.min(Integer.MAX_VALUE, settlement.wholeAmount());
			setLootRollCarry(settlement.carry());
		} else {
			desiredThrows = (int) Math.min(Integer.MAX_VALUE, Math.floor(expectedThrows));
			setLootRollCarry(0.0);
		}
		if (desiredThrows <= 0) return false;

        // 3. 受野外猎物储量限制
        double available = teamTown.maypickTerrainResource(TerrainResourceType.HUNT, desiredThrows);
        int actualThrows = Math.min(desiredThrows, (int) Math.floor(available));
        if (actualThrows <= 0) return false;

        // 4. 获取固定概率的战利品表。居民生产力只决定抽取次数，
		// 不再通过 Loot Luck 隐式改变物品组成。
        LootTable lootTable = world.getServer().getLootData()
                .getLootTable(new ResourceLocation(FHMain.MODID, "town/hunting"));
        if (lootTable == LootTable.EMPTY) {
            FHMain.LOGGER.error("Missing hunting loot table");
            return false;
        }

        LootParams lootParams = new LootParams.Builder(world)
                .withLuck(0.0f)
                .create(LootContextParamSets.EMPTY);

        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < actualThrows; i++) {
            lootTable.getRandomItems(lootParams, world.random.nextInt(), loot::add);
        }

// 合并同类物品并入库...
        // 合并同类物品
        Map<Item, Integer> merged = new HashMap<>();
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) continue;
            merged.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        // 5. 将战利品存入仓库
        for (Map.Entry<Item, Integer> entry : merged.entrySet()) {
            ItemStack batch = new ItemStack(entry.getKey(), entry.getValue());
            teamTown.getActionExecutorHandler().execute(
                    new TownResourceActions.ItemResourceAction(
                            batch,
                            ResourceActionType.ADD,
                            entry.getValue(),
                            ResourceActionMode.MAXIMIZE
                    )
            );
        }

        // 6. 扣除对应的野外猎物储量（无论仓库是否满，猎物已猎杀）
        teamTown.pickTerrainResource(TerrainResourceType.HUNT, actualThrows);
        return true;
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
		if(currentResidentNum >= getMaxResidents()) return NEGATIVE_INFINITY;
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		return config.assignmentBasePriority.get()
				- config.assignmentPenaltyPerWorker.get() * currentResidentNum
				+ config.assignmentFillRatioBonus.get() * currentResidentNum / getMaxResidents()
				+ config.assignmentRatingMultiplier.get() * rating;
	}

	@Override
	public double getResidentScore(Resident resident) {
		FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
		return TownMathFunctions.linearResidentProductivity(
				new double[]{
						resident.getHealth(),
						resident.getMental(),
						resident.getStrength(),
						resident.getIntelligence()
				},
				new double[]{
						config.healthWeight.get(),
						config.mentalWeight.get(),
						config.strengthWeight.get(),
						config.intelligenceWeight.get()
				},
				resident.getWorkProficiency(HuntingBaseBuilding.class),
				config.productivityAtAttributeZero.get(),
				config.productivityAtAttributeHundred.get(),
				config.maximumProficiency.get(),
				config.bonusAtMaximumProficiency.get(),
				config.minimumResidentProductivity.get(),
				config.maximumResidentProductivity.get()
		);
	}

	public double getEffectiveTemperature() {
		return temperature + temperatureModifier;
	}

	public static boolean isTemperatureValid(double effectiveTemperature){
		if (DEBUG_MODE) return true;
		return effectiveTemperature >= FHConfig.SERVER.TOWN.HUNTING.minimumWorkingTemperatureCelsius.get();
	}

	public boolean isTemperatureValid() {
		return isTemperatureValid(getEffectiveTemperature());
	}

	public boolean isSpaceValid(){
		return this.area >= FHConfig.SERVER.TOWN.HUNTING.minimumFloorAreaBlocks.get()
				&& this.volume >= FHConfig.SERVER.TOWN.HUNTING.minimumInteriorVolumeBlocks.get();
	}
}
