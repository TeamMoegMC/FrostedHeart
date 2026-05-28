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

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.action.*;
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
					Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid),
					OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.occupiedVolume),
					Codec.list(UUIDUtil.CODEC).optionalFieldOf("residentsID",List.of()).forGetter(o -> new ArrayList<>(o.residentsID)),
					Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.area),
					Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.volume),
					Codec.DOUBLE.optionalFieldOf("temperature",0D).forGetter(o -> o.temperature),
					Codec.INT.optionalFieldOf("maxResidents",0).forGetter(o -> o.maxResidents),
					Codec.INT.optionalFieldOf("tanningRackNum",0).forGetter(o -> o.tanningRackNum),
					Codec.DOUBLE.optionalFieldOf("temperatureModifier",null).forGetter(o -> o.temperatureModifier))
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
	 * @param occupiedVolume the occupied area
	 * @param residentsID list of resident UUIDs (will be converted to Set)
	 * @param area the area
	 * @param volume the volume
	 * @param temperature the temperature
	 * @param maxResidents the maximum residents
	 * @param tanningRackNum the number of tanning racks
	 * @param temperatureModifier the temperature modifier
	 */
	public HuntingBaseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, java.util.List<UUID> residentsID, int area, int volume, double temperature, int maxResidents, int tanningRackNum, double temperatureModifier) {
		super(pos);
		this.isStructureValid = isStructureValid;
		this.occupiedVolume = occupiedVolume;
		this.residentsID = new java.util.HashSet<>(residentsID);
		this.area = area;
		this.volume = volume;
		this.temperature = temperature;
		this.maxResidents = maxResidents;
		this.tanningRackNum = tanningRackNum;
		this.temperatureModifier = temperatureModifier;
	}

    //硬编码猎人收获评分
    private static final double THROWS_PER_SCORE = 2.0;

	@Override
	public boolean work(ITownWithBuildings town, ServerLevel world) {
        if (!(town instanceof TeamTown teamTown)) {
            FHMain.LOGGER.error("HuntingBaseBuilding.work: ITown is not TeamTown, need to fix work method.");//添加对其它城镇的适配
            throw new IllegalArgumentException("HuntingBaseBuilding ERROR: Can't work in non-team town :" + town);
        }

        // 1. 计算所有猎人的总技能评分
        double totalScore = 0.0;
        Collection<Resident> residents = this.getResidents(teamTown);
        for (Resident resident : residents) {
            if (resident == null) continue;
            double score = getResidentScore(resident);
            if (score > 0) totalScore += score;
        }

        // 2. 期望投掷次数
        int desiredThrows = (int) Math.floor(totalScore * THROWS_PER_SCORE);
        if (desiredThrows <= 0) desiredThrows = 1;

        // 3. 受野外猎物储量限制
        double available = teamTown.maypickTerrainResource(TerrainResourceType.HUNT, desiredThrows);
        int actualThrows = Math.min(desiredThrows, (int) Math.floor(available));
        if (actualThrows <= 0) return false;

        // 4. 计算幸运值（基于平均技能评分）
        int residentCount = this.residentsID.size();
        float luck = residentCount > 0 ? (float) (totalScore / residentCount * 0.1f) : 0.0f;

        // 5. 获取战利品表
        // 获取战利品表
        LootTable lootTable = world.getServer().getLootData()
                .getLootTable(new ResourceLocation(FHMain.MODID, "town/hunting"));
        if (lootTable == LootTable.EMPTY) {
            FHMain.LOGGER.error("Missing hunting loot table");
            return false;
        }

        LootParams lootParams = new LootParams.Builder(world)
                .withLuck(luck)
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

        // 6. 将战利品存入仓库
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

        // 7. 扣除对应的野外猎物储量（无论仓库是否满，猎物已猎杀）
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
		if(currentResidentNum >= maxResidents) return NEGATIVE_INFINITY;
		return -currentResidentNum + (double) currentResidentNum / maxResidents + 0.5/*the base priority of workerType*/ + rating;
	}

	@Override
	public double getResidentScore(Resident resident) {
        double healthScore = TownMathFunctions.attributeScore(resident.getHealth());
        double mentalScore = TownMathFunctions.attributeScore(resident.getMental());
        double strengthScore = TownMathFunctions.attributeScore(resident.getStrength());
        double intelligenceScore = TownMathFunctions.attributeScore(resident.getIntelligence());
        double geometricMean = Math.pow(
                healthScore * mentalScore * strengthScore * intelligenceScore, 0.25
        );
        double workProficiencyPart = 1.0 + 1.5 * TownMathFunctions.CalculatingFunction1(resident.getWorkProficiency(HuntingBaseBuilding.class));
        return geometricMean * workProficiencyPart;
	}

	public double getEffectiveTemperature() {
		return temperature + temperatureModifier;
	}

	public static boolean isTemperatureValid(double effectiveTemperature){
		if (DEBUG_MODE) return true;
		return effectiveTemperature >= TownMathFunctions.WORKING_TEMP;
	}

	public boolean isTemperatureValid() {
		return isTemperatureValid(getEffectiveTemperature());
	}

	public boolean isSpaceValid(){
		return this.area >= 4 && this.volume >= 8;
	}
}
