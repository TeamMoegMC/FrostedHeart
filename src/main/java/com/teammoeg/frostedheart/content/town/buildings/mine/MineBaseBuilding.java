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
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.UUIDUtil;
import lombok.Getter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class MineBaseBuilding extends AbstractTownResidentWorkBuilding {
    public record MiningDailyReport(
            boolean hasData,
            double requested,
            double extracted,
            List<TownProductionReportItem> items,
            TownProductionStopReason stopReason
    ) {
        public static final MiningDailyReport EMPTY =
                new MiningDailyReport(false, 0.0, 0.0, List.of(), TownProductionStopReason.NONE);
        public static final Codec<MiningDailyReport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("hasData", false).forGetter(MiningDailyReport::hasData),
                Codec.DOUBLE.optionalFieldOf("requested", 0.0).forGetter(MiningDailyReport::requested),
                Codec.DOUBLE.optionalFieldOf("extracted", 0.0).forGetter(MiningDailyReport::extracted),
                TownProductionReportItem.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(MiningDailyReport::items),
                TownProductionStopReason.CODEC.optionalFieldOf("stopReason", TownProductionStopReason.NONE)
                        .forGetter(MiningDailyReport::stopReason)
        ).apply(instance, MiningDailyReport::new));

        public MiningDailyReport {
            requested = sanitize(requested);
            extracted = Math.min(requested, sanitize(extracted));
            items = List.copyOf(items);
            stopReason = stopReason == null ? TownProductionStopReason.NONE : stopReason;
        }

        public double stored() {
            return items.stream().mapToDouble(TownProductionReportItem::stored).sum();
        }

        public double lost() {
            return items.stream().mapToDouble(TownProductionReportItem::lost).sum();
        }
    }

    public record MiningForecast(
            double totalProductivity,
            double requested,
            double extractable,
            TownProductionStopReason stopReason
    ) {}

	public static final Codec<MineBaseBuilding> CODEC = RecordCodecBuilder.create(t -> t.group(
                    BlockPos.CODEC.optionalFieldOf("pos",BlockPos.ZERO).forGetter(o -> o.pos),
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(o -> o.isInitialized()),
                    Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false).forGetter(o -> o.isOccupiedAreaOverlapped()),
                    Codec.BOOL.optionalFieldOf("isStructureValid",false).forGetter(o -> o.isStructureValid()),
                    OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume",OccupiedVolume.EMPTY).forGetter(o -> o.getOccupiedVolume()),
                    Codec.list(UUIDUtil.CODEC).optionalFieldOf("residentsID",List.of()).forGetter(o -> new ArrayList<>(o.getResidentsID())),
                    Codec.INT.optionalFieldOf("area",0).forGetter(o -> o.getArea()),
                    Codec.INT.optionalFieldOf("volume",0).forGetter(o -> o.getVolume()),

					Codec.INT.optionalFieldOf("maxResidents",0).forGetter(o -> o.getMaxResidents()),

                    Codec.list(BlockPos.CODEC).optionalFieldOf("linkedMines", new ArrayList<>())
                            .forGetter(o -> o.getLinkedMines() == null ? new ArrayList<>() : new ArrayList<>(o.getLinkedMines())),
                    MiningDailyReport.CODEC.optionalFieldOf("dailyReport", MiningDailyReport.EMPTY)
                            .forGetter(MineBaseBuilding::getDailyReport)
			)
			.apply(t, MineBaseBuilding::new));

	@Getter
	private int area;
	@Getter
	private int volume;

    public Set<BlockPos> linkedMines = new HashSet<>();
    @Getter
    private MiningDailyReport dailyReport = MiningDailyReport.EMPTY;

	public void setArea(int area) { this.area = area; fireChange(); }
	public void setVolume(int volume) { this.volume = volume; fireChange(); }


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
	public MineBaseBuilding(BlockPos pos, boolean initialized, boolean occupiedAreaOverlapped,
                            boolean isStructureValid, OccupiedVolume occupiedVolume,
                            java.util.List<UUID> residentsID, int area, int volume,
                            int maxResidents, List<BlockPos> linkedMines,
                            MiningDailyReport dailyReport) {
		super(pos);
        this.setInitialized(initialized);
        this.setOccupiedAreaOverlapped(occupiedAreaOverlapped);
		this.setIsStructureValid(isStructureValid);
		this.setOccupiedVolume(occupiedVolume);
		this.residentsID = new HashSet<>(residentsID);
		this.setArea(area);
		this.setVolume(volume);
        this.setMaxResidents(maxResidents);
        this.linkedMines = new HashSet<>(linkedMines);
        this.dailyReport = dailyReport == null ? MiningDailyReport.EMPTY : dailyReport;
	}

	@Override
	public boolean work(ITownWithBuildings town) {

        if (!(town instanceof TeamTown teamTown)) {
            throw new IllegalArgumentException("MineBaseBuilding ERROR: Can't work in non-team town :" + town);
        }

        List<Resident> workingResidents = residentsID.stream()
                .map(id -> teamTown.getResident(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        // 1. Requested output, measured in item units per town day.
        double totalProductivity = 0.0;
        for (Resident r : workingResidents) {
            double productivity = getResidentScore(r);
            if (productivity > 0.0) totalProductivity += productivity;
        }
        double requestedOutputPerDay = MiningDailyModel.requestedOutput(
                totalProductivity,
                FHConfig.SERVER.TOWN.MINING.baseOutputPerStandardWorkerDay.get());
        if (requestedOutputPerDay <= 0.0) {
            setDailyReport(new MiningDailyReport(true, 0.0, 0.0, List.of(),
                    TownProductionStopReason.NO_ELIGIBLE_WORKERS));
            return false;
        }

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
        if (grandTotal <= 0.0) {
            setDailyReport(new MiningDailyReport(true, requestedOutputPerDay, 0.0, List.of(),
                    TownProductionStopReason.NO_USABLE_MINES));
            return false;
        }

        // 3. 逐区块开采
        boolean performedWork = false;
        double extractedTotal = 0.0;
        Map<Item, double[]> reportAmounts = new HashMap<>();
        for (Map.Entry<ChunkPos, Map<Item, Integer>> entry : chunkWeights.entrySet()) {
            ChunkPos chunk = entry.getKey();
            Map<Item, Integer> weights = entry.getValue();
            double weightSum = chunkTotalWeight.get(chunk);
            double desired = MiningDailyModel.weightedShare(
                    requestedOutputPerDay, weightSum, grandTotal);

            // 使用 TeamTown 的封装方法
            double actual = teamTown.pickTerrainResource(TerrainResourceType.ORE, chunk, desired);
            if (actual <= 0.0) continue;
            performedWork = true;
            extractedTotal += actual;

            for (Map.Entry<Item, Integer> wEntry : weights.entrySet()) {
                Item item = wEntry.getKey();
                double itemAmount = MiningDailyModel.weightedShare(
                        actual, wEntry.getValue(), weightSum);
                TownResourceActionResults.ItemResourceActionResult result =
                        teamTown.getActionExecutorHandler().execute(
                        new TownResourceActions.ItemResourceAction(
                                new ItemStack(item), ResourceActionType.ADD, itemAmount, ResourceActionMode.ATTEMPT
                        )
                );
                double[] amounts = reportAmounts.computeIfAbsent(item, ignored -> new double[2]);
                amounts[0] += itemAmount;
                amounts[1] += result.modifiedAmount();
            }
        }
        TownProductionStopReason stopReason =
                performedWork && extractedTotal + 1.0e-9 >= requestedOutputPerDay
                        ? TownProductionStopReason.NONE
                        : TownProductionStopReason.TERRAIN_DEPLETED;
        setDailyReport(new MiningDailyReport(
                true,
                requestedOutputPerDay,
                extractedTotal,
                toReportItems(reportAmounts),
                stopReason
        ));
        if (performedWork) {
            FHConfig.Server.Town.ResidentProgression progression =
                    FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
            for (Resident resident : workingResidents) {
                resident.recordDailyActivity(
                        FHConfig.SERVER.TOWN.MINING.physicalActivity.get(),
                        FHConfig.SERVER.TOWN.MINING.learningActivity.get());
                resident.gainDailyWorkProficiency(
                        MineBaseBuilding.class,
                        progression.proficiencyGrowthAtZeroPerWorkday.get(),
                        progression.minimumProficiencyGrowthPerWorkday.get());
            }
        }
		return performedWork;
	}

    public MiningForecast getForecast(TeamTown town) {
        double totalProductivity = residentsID.stream()
                .map(id -> town.getResident(id).orElse(null))
                .filter(Objects::nonNull)
                .filter(this::canResidentWork)
                .mapToDouble(this::getResidentScore)
                .filter(value -> value > 0.0)
                .sum();
        double requested = MiningDailyModel.requestedOutput(
                totalProductivity,
                FHConfig.SERVER.TOWN.MINING.baseOutputPerStandardWorkerDay.get());
        if (requested <= 0.0) {
            return new MiningForecast(totalProductivity, 0.0, 0.0,
                    TownProductionStopReason.NO_ELIGIBLE_WORKERS);
        }

        Map<ChunkPos, Double> chunkWeights = new HashMap<>();
        double totalWeight = 0.0;
        for (BlockPos minePos : linkedMines) {
            ITownBuilding building = town.getTownBuilding(minePos).orElse(null);
            if (!(building instanceof MineBuilding mine) || !mine.isBuildingWorkable()) continue;
            double weight = MineBuilding.getWeights(mine.getBiomePath()).values().stream()
                    .mapToDouble(Integer::doubleValue).sum();
            if (weight <= 0.0) continue;
            chunkWeights.merge(new ChunkPos(minePos), weight, Double::sum);
            totalWeight += weight;
        }
        if (totalWeight <= 0.0) {
            return new MiningForecast(totalProductivity, requested, 0.0,
                    TownProductionStopReason.NO_USABLE_MINES);
        }

        double extractable = 0.0;
        for (Map.Entry<ChunkPos, Double> entry : chunkWeights.entrySet()) {
            double desired = MiningDailyModel.weightedShare(
                    requested, entry.getValue(), totalWeight);
            extractable += Math.min(desired,
                    town.getRemainingTerrainResource(TerrainResourceType.ORE, entry.getKey()));
        }
        return new MiningForecast(totalProductivity, requested, extractable,
                extractable + 1.0e-9 >= requested
                        ? TownProductionStopReason.NONE
                        : TownProductionStopReason.TERRAIN_DEPLETED);
    }

    private void setDailyReport(MiningDailyReport dailyReport) {
        this.dailyReport = dailyReport == null ? MiningDailyReport.EMPTY : dailyReport;
        fireChange();
    }

    private static List<TownProductionReportItem> toReportItems(Map<Item, double[]> amounts) {
        return amounts.entrySet().stream()
                .map(entry -> new TownProductionReportItem(
                        entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparingDouble(TownProductionReportItem::produced).reversed()
                        .thenComparing(item -> item.item().getDescriptionId()))
                .toList();
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    @Override
    public double getResidentScore(Resident resident) {
        FHConfig.Server.Town.Mining config = FHConfig.SERVER.TOWN.MINING;
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
                resident.getWorkProficiency(MineBaseBuilding.class),
                config.productivityAtAttributeZero.get(),
                config.productivityAtAttributeHundred.get(),
                config.maximumProficiency.get(),
                config.bonusAtMaximumProficiency.get(),
                config.minimumResidentProductivity.get(),
                config.maximumResidentProductivity.get()
        );
    }

    public void clearLinkedMines() {
        if (linkedMines != null && !linkedMines.isEmpty()) {
            linkedMines.clear();
            fireChange();
        }
    }

    public void addLinkedMine(BlockPos pos) {
        if (linkedMines == null) {
            linkedMines = new HashSet<>();
        }
        if (linkedMines.add(pos)) {
            fireChange();
        }
    }

    public void removeLinkedMine(BlockPos pos) {
        if (linkedMines != null && linkedMines.remove(pos)) {
            fireChange();
        }
    }

    public Set<BlockPos> getLinkedMines() {
        return linkedMines;
    }

    public int getConnectionRadius() {
        return FHConfig.SERVER.TOWN.MINING.connectionRadiusBlocks.get();
    }
}
