/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActionResults;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Comparator;

/**
 * 货运站的持久化建筑状态。
 * <p>
 * Persistent building state and daily transport production for one station.
 */
public class TransportStationBuilding extends AbstractTownResidentWorkBuilding {
    public record TransportStationDailyReport(
            boolean hasData,
            int workerCount,
            double totalProductivity,
            double plannedCapacity,
            double addedCapacity,
            TownProductionStopReason stopReason
    ) {
        public static final TransportStationDailyReport EMPTY = new TransportStationDailyReport(
                false, 0, 0.0, 0.0, 0.0, TownProductionStopReason.NONE);
        public static final Codec<TransportStationDailyReport> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("hasData", false).forGetter(TransportStationDailyReport::hasData),
                Codec.INT.optionalFieldOf("workerCount", 0).forGetter(TransportStationDailyReport::workerCount),
                Codec.DOUBLE.optionalFieldOf("totalProductivity", 0.0).forGetter(TransportStationDailyReport::totalProductivity),
                Codec.DOUBLE.optionalFieldOf("plannedCapacity", 0.0).forGetter(TransportStationDailyReport::plannedCapacity),
                Codec.DOUBLE.optionalFieldOf("addedCapacity", 0.0).forGetter(TransportStationDailyReport::addedCapacity),
                TownProductionStopReason.CODEC.optionalFieldOf("stopReason", TownProductionStopReason.NONE)
                        .forGetter(TransportStationDailyReport::stopReason)
        ).apply(instance, TransportStationDailyReport::new));

        public TransportStationDailyReport {
            workerCount = Math.max(0, workerCount);
            totalProductivity = sanitize(totalProductivity);
            plannedCapacity = sanitize(plannedCapacity);
            addedCapacity = Math.min(plannedCapacity, sanitize(addedCapacity));
            stopReason = stopReason == null ? TownProductionStopReason.NONE : stopReason;
        }
    }

    /** Predicted result for the next town settlement before resource mutation. */
    public record TransportStationForecast(
            int workerCount,
            double totalProductivity,
            double plannedCapacity,
            TownProductionStopReason stopReason
    ) {
        public TransportStationForecast {
            workerCount = Math.max(0, workerCount);
            totalProductivity = sanitize(totalProductivity);
            plannedCapacity = sanitize(plannedCapacity);
            stopReason = stopReason == null ? TownProductionStopReason.NONE : stopReason;
        }
    }

    public static final Codec<TransportStationBuilding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("pos", BlockPos.ZERO).forGetter(building -> building.pos),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(TransportStationBuilding::isInitialized),
            Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false)
                    .forGetter(TransportStationBuilding::isOccupiedAreaOverlapped),
            Codec.BOOL.optionalFieldOf("isStructureValid", false)
                    .forGetter(TransportStationBuilding::isStructureValid),
            OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume", OccupiedVolume.EMPTY)
                    .forGetter(TransportStationBuilding::getOccupiedVolume),
            UUIDUtil.CODEC.listOf().optionalFieldOf("residentsID", List.of())
                    .forGetter(building -> building.getResidentsID().stream().sorted().toList()),
            Codec.INT.optionalFieldOf("area", 0).forGetter(TransportStationBuilding::getArea),
            Codec.INT.optionalFieldOf("volume", 0).forGetter(TransportStationBuilding::getVolume),
            Codec.INT.optionalFieldOf("maxResidents", 0).forGetter(TransportStationBuilding::getMaxResidents),
            TransportStationDailyReport.CODEC.optionalFieldOf("dailyReport", TransportStationDailyReport.EMPTY)
                    .forGetter(TransportStationBuilding::getDailyReport)
    ).apply(instance, TransportStationBuilding::new));

    @Getter
    private int area;
    @Getter
    private int volume;
    @Getter
    private TransportStationDailyReport dailyReport = TransportStationDailyReport.EMPTY;

    public TransportStationBuilding(BlockPos pos) {
        super(pos);
    }

    public TransportStationBuilding(
            BlockPos pos,
            boolean initialized,
            boolean occupiedAreaOverlapped,
            boolean structureValid,
            OccupiedVolume occupiedVolume,
            List<UUID> residentsID,
            int area,
            int volume,
            int maxResidents
    ) {
        this(pos, initialized, occupiedAreaOverlapped, structureValid, occupiedVolume,
                residentsID, area, volume, maxResidents, TransportStationDailyReport.EMPTY);
    }

    public TransportStationBuilding(
            BlockPos pos,
            boolean initialized,
            boolean occupiedAreaOverlapped,
            boolean structureValid,
            OccupiedVolume occupiedVolume,
            List<UUID> residentsID,
            int area,
            int volume,
            int maxResidents,
            TransportStationDailyReport dailyReport
    ) {
        super(pos);
        setInitialized(initialized);
        setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        setIsStructureValid(structureValid);
        setOccupiedVolume(occupiedVolume);
        this.residentsID = new HashSet<>(residentsID);
        setArea(area);
        setVolume(volume);
        setMaxResidents(maxResidents);
        this.dailyReport = dailyReport == null ? TransportStationDailyReport.EMPTY : dailyReport;
    }

    public void setArea(int area) {
        if (this.area == area) return;
        this.area = area;
        fireChange();
    }

    public void setVolume(int volume) {
        if (this.volume == volume) return;
        this.volume = volume;
        fireChange();
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable() && isSpaceValid();
    }

    public boolean isSpaceValid() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        return area >= config.minimumFloorAreaBlocks.get()
                && volume >= config.minimumInteriorVolumeBlocks.get();
    }

    @Override
    public boolean shouldRunDailySettlement() {
        return true;
    }

    @Override
    public boolean work(ITownWithBuildings town, ServerLevel world) {
        if (!(town instanceof TeamTown teamTown)) {
            throw new IllegalArgumentException("TransportStationBuilding requires a TeamTown.");
        }
        if (!isBuildingWorkable()) {
            setDailyReport(new TransportStationDailyReport(
                    true, 0, 0.0, 0.0, 0.0,
                    TownProductionStopReason.BUILDING_UNWORKABLE));
            return false;
        }

        List<Resident> workingResidents = eligibleWorkers(teamTown);
        TransportStationDailyModel.DailyResult result = calculateDailyResult(workingResidents);
        if (result.workerCount() == 0) {
            setDailyReport(new TransportStationDailyReport(
                    true, 0, 0.0, 0.0, 0.0,
                    TownProductionStopReason.NO_ELIGIBLE_WORKERS));
            return false;
        }
        if (result.producedCapacity() <= TeamTownResourceHolder.DELTA) {
            setDailyReport(new TransportStationDailyReport(
                    true, result.workerCount(), result.totalProductivity(),
                    0.0, 0.0, TownProductionStopReason.OUTPUT_DISABLED));
            return false;
        }

        TownResourceActionResults.VirtualResourceAttributeActionResult actionResult =
                teamTown.getActionExecutorHandler().execute(
                        new TownResourceActions.VirtualResourceAttributeAction(
                                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0),
                                result.producedCapacity(),
                                ResourceActionType.ADD,
                                ResourceActionMode.ATTEMPT));
        double added = actionResult.modifiedAmount();
        if (added > TeamTownResourceHolder.DELTA) {
            grantDailyTransportProficiency(workingResidents);
        }
        setDailyReport(new TransportStationDailyReport(
                true, result.workerCount(), result.totalProductivity(),
                result.producedCapacity(), added,
                actionResult.allModified()
                        ? TownProductionStopReason.NONE
                        : TownProductionStopReason.RESOURCE_REJECTED));
        return added > TeamTownResourceHolder.DELTA;
    }

    @Override
    public double getResidentScore(Resident resident) {
        if (resident == null) return 0.0;
        return TransportStationDailyModel.residentProductivity(
                workerInput(resident), productionParameters());
    }

    /** Returns this resident's planned daily transport capacity contribution. */
    public double getResidentCapacityContribution(Resident resident) {
        return TransportStationDailyModel.producedCapacity(
                getResidentScore(resident),
                productionParameters().transportCapacityPerStandardWorkerDay());
    }

    /**
     * Calculates the same station-local result used by the next settlement.
     * This does not reserve capacity or mutate residents and resources.
     */
    public TransportStationForecast getForecast(TeamTown town) {
        if (!isBuildingWorkable()) {
            return new TransportStationForecast(
                    0, 0.0, 0.0, TownProductionStopReason.BUILDING_UNWORKABLE);
        }
        TransportStationDailyModel.DailyResult result = calculateDailyResult(eligibleWorkers(town));
        if (result.workerCount() == 0) {
            return new TransportStationForecast(
                    0, 0.0, 0.0, TownProductionStopReason.NO_ELIGIBLE_WORKERS);
        }
        if (result.producedCapacity() <= TeamTownResourceHolder.DELTA) {
            return new TransportStationForecast(
                    result.workerCount(), result.totalProductivity(), 0.0,
                    TownProductionStopReason.OUTPUT_DISABLED);
        }
        return new TransportStationForecast(
                result.workerCount(), result.totalProductivity(), result.producedCapacity(),
                TownProductionStopReason.NONE);
    }

    private void setDailyReport(TransportStationDailyReport dailyReport) {
        TransportStationDailyReport next = dailyReport == null
                ? TransportStationDailyReport.EMPTY
                : dailyReport;
        if (next.equals(this.dailyReport)) return;
        this.dailyReport = next;
        fireChange();
    }

    private static TransportStationDailyModel.WorkerInput workerInput(Resident resident) {
        return new TransportStationDailyModel.WorkerInput(
                resident.getHealth(),
                resident.getMental(),
                resident.getStrength(),
                resident.getIntelligence(),
                resident.getWorkProficiency(TransportStationBuilding.class));
    }

    private List<Resident> eligibleWorkers(TeamTown town) {
        if (town == null) return List.of();
        return this.getResidents(town).stream()
                .filter(Objects::nonNull)
                .filter(this::canResidentWork)
                .sorted(Comparator.comparing(Resident::getUUID))
                .toList();
    }

    private static TransportStationDailyModel.DailyResult calculateDailyResult(
            List<Resident> workingResidents
    ) {
        return TransportStationDailyModel.calculate(
                workingResidents.stream().map(TransportStationBuilding::workerInput).toList(),
                productionParameters());
    }

    private static TransportStationDailyModel.Parameters productionParameters() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        return new TransportStationDailyModel.Parameters(
                config.transportCapacityPerStandardWorkerDay.get(),
                config.healthWeight.get(),
                config.mentalWeight.get(),
                config.strengthWeight.get(),
                config.intelligenceWeight.get(),
                config.productivityAtAttributeZero.get(),
                config.productivityAtAttributeHundred.get(),
                config.maximumProficiency.get(),
                config.bonusAtMaximumProficiency.get(),
                config.minimumResidentProductivity.get(),
                config.maximumResidentProductivity.get());
    }

    private static void grantDailyTransportProficiency(List<Resident> workingResidents) {
        FHConfig.Server.Town.ResidentProgression progression =
                FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        for (Resident resident : workingResidents) {
            resident.gainDailyWorkProficiency(
                    TransportStationBuilding.class,
                    progression.proficiencyGrowthAtZeroPerWorkday.get(),
                    progression.minimumProficiencyGrowthPerWorkday.get());
        }
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
