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
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.IActionExecutorHandler;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceData;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
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

    private int connectionRadius = 1024;
    public Set<BlockPos> linkedMines;
    private static final double BASE_PER_SCORE = 4.0;


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
		this.residentsID = new java.util.HashSet<>(residentsID);
		this.area = area;
		this.volume = volume;
		this.maxResidents = maxResidents;
        this.linkedMines = new java.util.HashSet<>(linkedMines);
	}

	@Override
	public boolean work(ITownWithBuildings town) {
		/*
		if (town instanceof TeamTown teamTown) {
			double toModify=0;
			for(UUID residentID : residentsID) {
				Resident resident=teamTown.getResident(residen  tID).orElse(null);
				if(resident==null)continue;
				double efficiency=0.2 * getResidentScore(resident);
				if(efficiency<=0)continue;
//				efficiency = 2.0;
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
											.filter(TownResourceActionResults.ItemResourceActionResult::allModified)
					.mapToDouble(TownResourceActionResults.ItemResourceActionResult::modifiedAmount)
					.sum();
			teamTown.pickTerrainResource(TerrainResourceType.ORE, modified);
			return true;
		}
		throw new IllegalArgumentException("MineBaseBuilding ERROR: Can't work in non-team town :" + town);*/

        if (!(town instanceof TeamTown teamTown)) {
            throw new IllegalArgumentException("MineBaseBuilding ERROR: Can't work in non-team town :" + town);
        }

        // 1. 居民效率
        double totalEfficiency = 0.0;
        for (UUID id : residentsID) {
            Resident r = teamTown.getResident(id).orElse(null);
            if (r == null) continue;
            double eff = BASE_PER_SCORE * getResidentScore(r);
            if (eff > 0) totalEfficiency += eff;
        }
        if (totalEfficiency <= 0.0) return false;

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
            double desired = totalEfficiency * weightSum / grandTotal;

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
		//double rating = state.getRating();
		return -currentResidentNum + 1.0 * currentResidentNum / maxResidents + 0.4/*the base priority of workerType*/;
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
        double workProficiencyPart = 1.0 + 1.5 * TownMathFunctions.CalculatingFunction1(resident.getWorkProficiency(MineBaseBuilding.class));
        return geometricMean * workProficiencyPart;
    }

    public void clearLinkedMines() { linkedMines.clear(); }
    public void addLinkedMine(BlockPos pos) { linkedMines.add(pos); }

    public Set<BlockPos> getLinkedMines() {
        return linkedMines;
    }

    public int getConnectionRadius() { return connectionRadius; }
}
