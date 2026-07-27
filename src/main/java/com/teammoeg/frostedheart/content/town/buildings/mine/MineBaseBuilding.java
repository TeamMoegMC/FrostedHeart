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
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.UUIDUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

import static java.lang.Double.NEGATIVE_INFINITY;

public class MineBaseBuilding extends AbstractTownResidentWorkBuilding {
	public static final Codec<MineBaseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
                    Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid),
                    OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.occupiedVolume),
                    Codec.list(UUIDUtil.CODEC).optionalFieldOf("residentsID",List.of()).forGetter(o -> new ArrayList<>(o.residentsID)),
                    Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.area),
                    Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.volume),

					Codec.INT.optionalFieldOf("maxResidents",0).forGetter(o -> o.maxResidents),

                    Codec.list(BlockPos.CODEC).optionalFieldOf("linkedMines", new ArrayList<>())
                            .forGetter(o -> o.linkedMines == null ? new ArrayList<>() : new ArrayList<>(o.linkedMines))
			)
			.apply(t, MineBaseBuilding::new));

	public int area;

	public int volume;

    public Set<BlockPos> linkedMines = new HashSet<>();
    /**
     * Unit definition for config values: a standard worker has each core
     * attribute at 50 and zero mining proficiency.
     */
    private static final double STANDARD_WORKER_ATTRIBUTE_SCORE = TownMathFunctions.attributeScore(50.0);


	public MineBaseBuilding(BlockPos pos) {
		super(pos);
	}

	@Override
	public boolean isBuildingWorkable() {
		return super.isBuildingWorkable();
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
	 * @param maxResidents the maximum residents
	 */
	public MineBaseBuilding(BlockPos pos, boolean isStructureValid, OccupiedVolume occupiedVolume, java.util.List<UUID> residentsID, int area, int volume, int maxResidents,List<BlockPos> linkedMines) {
		super(pos);
		this.isStructureValid = isStructureValid;
		this.occupiedVolume = occupiedVolume;
		this.residentsID = new HashSet<>(residentsID);
		this.area = area;
		this.volume = volume;
		this.maxResidents = maxResidents;
        this.linkedMines = new HashSet<>(linkedMines);
	}

	@Override
	public boolean work(ITownWithBuildings town) {

        if (!(town instanceof TeamTown teamTown)) {
            throw new IllegalArgumentException("MineBaseBuilding ERROR: Can't work in non-team town :" + town);
        }

        // 1. Requested output, measured in item units per town day.
        double requestedOutputPerDay = 0.0;
        for (UUID id : residentsID) {
            Resident r = teamTown.getResident(id).orElse(null);
            if (r == null) continue;
            double workerOutputPerDay = FHConfig.SERVER.TOWN.MINING.baseOutputPerStandardWorkerDay.get()
                    * getResidentScore(r);
            if (workerOutputPerDay > 0) requestedOutputPerDay += workerOutputPerDay;
        }
        if (requestedOutputPerDay <= 0.0) return false;

        // 2. 收集有效矿场并按区块分组
        Map<ChunkPos, Double> chunkTotalWeight = new HashMap<>();
        Map<ChunkPos, Map<Item, Integer>> chunkWeights = new HashMap<>();
        double grandTotal = 0.0;

        for (BlockPos minePos : linkedMines) {
            ITownBuilding b = teamTown.getTownBuilding(minePos).orElse(null);
            if (!(b instanceof MineBuilding mine) || !mine.isBuildingWorkable()) continue;

            Map<Item, Integer> weights = MineBuilding.getWeights(mine.getBiomePath());
            int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
            if (sum <= 0) continue;

            ChunkPos chunk = new ChunkPos(minePos);

            chunkWeights.compute(chunk, (k, existing) -> {
                if (existing == null) {
                    return new HashMap<>(weights);
                }
                weights.forEach((item, w) -> existing.merge(item, w, Integer::sum));
                return existing;
            });

            chunkTotalWeight.merge(chunk, (double) sum, Double::sum);
            grandTotal += sum;
        }
        if (grandTotal <= 0.0) return false;

        // 3. 逐区块开采
        for (Map.Entry<ChunkPos, Map<Item, Integer>> entry : chunkWeights.entrySet()) {
            ChunkPos chunk = entry.getKey();
            Map<Item, Integer> weights = entry.getValue();
            double weightSum = chunkTotalWeight.get(chunk);
            double desired = requestedOutputPerDay * weightSum / grandTotal;

            // 使用 TeamTown 的封装方法
            double actual = teamTown.pickTerrainResource(TerrainResourceType.ORE, chunk, desired);
            if (actual <= 0.0) continue;

            for (Map.Entry<Item, Integer> wEntry : weights.entrySet()) {
                Item item = wEntry.getKey();
                double itemAmount = actual * wEntry.getValue() / weightSum;
                teamTown.getActionExecutorHandler().execute(
                        new TownResourceActions.ItemResourceAction(
                                new ItemStack(item), ResourceActionType.ADD, itemAmount, ResourceActionMode.ATTEMPT
                        )
                );
            }
        }
		return true;
	}

	@Override
	public double getResidentPriority() {
		if(!this.isBuildingWorkable()) return NEGATIVE_INFINITY;
		int currentResidentNum = this.residentsID.size();
		if(currentResidentNum >= maxResidents) return NEGATIVE_INFINITY;
        FHConfig.Server.Town.Mining config = FHConfig.SERVER.TOWN.MINING;
		return config.assignmentBasePriority.get()
                - config.assignmentPenaltyPerWorker.get() * currentResidentNum
                + config.assignmentFillRatioBonus.get() * currentResidentNum / maxResidents;
	}

    @Override
    public double getResidentScore(Resident resident) {
        FHConfig.Server.Town.Mining config = FHConfig.SERVER.TOWN.MINING;
        double healthScore = TownMathFunctions.attributeScore(resident.getHealth());
        double mentalScore = TownMathFunctions.attributeScore(resident.getMental());
        double strengthScore = TownMathFunctions.attributeScore(resident.getStrength());
        double intelligenceScore = TownMathFunctions.attributeScore(resident.getIntelligence());

        double weightedAttributeScore = weightedGeometricMean(
                new double[]{healthScore, mentalScore, strengthScore, intelligenceScore},
                new double[]{
                        config.healthWeight.get(),
                        config.mentalWeight.get(),
                        config.strengthWeight.get(),
                        config.intelligenceWeight.get()
                }
        );
        double attributeProductivity = weightedAttributeScore / STANDARD_WORKER_ATTRIBUTE_SCORE;

        double proficiency = Math.max(0.0, resident.getWorkProficiency(MineBaseBuilding.class));
        double proficiencyPart = 1.0 + config.maximumProficiencyBonus.get()
                * (1.0 - Math.exp(-proficiency * config.proficiencyCurvePerPoint.get()));
        return attributeProductivity * proficiencyPart;
    }

    private static double weightedGeometricMean(double[] scores, double[] weights) {
        double totalWeight = 0.0;
        double weightedLogSum = 0.0;
        for (int i = 0; i < scores.length; i++) {
            double weight = weights[i];
            if (weight <= 0.0) continue;
            if (scores[i] <= 0.0) return 0.0;
            totalWeight += weight;
            weightedLogSum += weight * Math.log(scores[i]);
        }
        if (totalWeight <= 0.0) {
            return STANDARD_WORKER_ATTRIBUTE_SCORE;
        }
        return Math.exp(weightedLogSum / totalWeight);
    }

    public void clearLinkedMines() {
        if (linkedMines != null) {
            linkedMines.clear();
        }
    }

    public void addLinkedMine(BlockPos pos) {
        if (linkedMines == null) {
            linkedMines = new HashSet<>();
        }
        linkedMines.add(pos);
    }

    public Set<BlockPos> getLinkedMines() {
        return linkedMines;
    }

    public int getConnectionRadius() {
        return FHConfig.SERVER.TOWN.MINING.connectionRadiusBlocks.get();
    }
}
