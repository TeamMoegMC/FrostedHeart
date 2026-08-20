/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentActivity;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTownTransportSettlementTest {
    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void singleStationForecastMatchesActionResultAndDailyReport() {
        BlockPos pos = new BlockPos(1, 64, 1);
        Resident worker = standardWorker("Single", pos);
        TransportStationBuilding station = workableStation(pos, worker);
        TeamTownResourceHolder resources = new TeamTownResourceHolder();
        TeamTownData data = townData(
                resources,
                Map.of(pos, station),
                Map.of(worker.getUUID(), worker));
        TransportStationBuilding.TransportStationForecast forecast =
                station.getForecast(data.createTeamTown());

        data.buildingsWork(null);

        assertEquals(1, forecast.workerCount());
        assertEquals(64.0, forecast.plannedCapacity(), EPSILON);
        assertEquals(forecast.plannedCapacity(), station.getDailyReport().plannedCapacity(), EPSILON);
        assertEquals(transportCapacity(resources), station.getDailyReport().addedCapacity(), EPSILON);
        assertEquals(TownProductionStopReason.NONE, station.getDailyReport().stopReason());
    }

    @Test
    void oneStationAggregatesMultipleEligibleWorkers() {
        BlockPos pos = new BlockPos(1, 64, 1);
        Resident first = standardWorker("First", pos);
        Resident second = standardWorker("Second", pos);
        TransportStationBuilding station = workableStation(pos, List.of(first, second));
        TeamTownResourceHolder resources = new TeamTownResourceHolder();
        TeamTownData data = townData(
                resources,
                Map.of(pos, station),
                Map.of(first.getUUID(), first, second.getUUID(), second));

        data.buildingsWork(null);

        assertEquals(2, station.getDailyReport().workerCount());
        assertEquals(2.0, station.getDailyReport().totalProductivity(), EPSILON);
        assertEquals(128.0, station.getDailyReport().addedCapacity(), EPSILON);
        assertEquals(128.0, transportCapacity(resources), EPSILON);
    }

    @Test
    void morningSettlementResetsOldCapacityAggregatesStationsAndGrantsProficiency() {
        BlockPos firstPos = new BlockPos(1, 64, 1);
        BlockPos secondPos = new BlockPos(10, 64, 1);
        Resident firstWorker = standardWorker("First", firstPos);
        Resident secondWorker = standardWorker("Second", secondPos);
        TransportStationBuilding firstStation = workableStation(firstPos, firstWorker);
        TransportStationBuilding secondStation = workableStation(secondPos, secondWorker);
        Map<BlockPos, ITownBuilding> buildings = new LinkedHashMap<>();
        buildings.put(firstPos, firstStation);
        buildings.put(secondPos, secondStation);
        TeamTownResourceHolder resources = new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 999.0));
        TeamTownData data = townData(
                resources,
                buildings,
                Map.of(firstWorker.getUUID(), firstWorker, secondWorker.getUUID(), secondWorker));

        data.buildingsWork(null);

        assertEquals(128.0, transportCapacity(resources), EPSILON);
        assertEquals(64.0, firstStation.getDailyReport().addedCapacity(), EPSILON);
        assertEquals(64.0, secondStation.getDailyReport().addedCapacity(), EPSILON);
        assertEquals(2.0, firstStation.getDailyReport().totalProductivity()
                + secondStation.getDailyReport().totalProductivity(), EPSILON);
        assertEquals(128.0,
                data.getTransportState().getDailyReport().totalCapacity(), EPSILON);
        assertEquals(0.0,
                data.getTransportState().getDailyReport().reservedCapacity(), EPSILON);
        assertTrue(data.getTransportState().getDailyReport().hasData());
        double expectedGrowth = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION
                .proficiencyGrowthAtZeroPerWorkday.get();
        assertEquals(expectedGrowth,
                firstWorker.getWorkProficiency(TransportStationBuilding.class), EPSILON);
        assertEquals(expectedGrowth,
                secondWorker.getWorkProficiency(TransportStationBuilding.class), EPSILON);
        ResidentActivity expectedActivity = new ResidentActivity(
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.physicalActivity.get(),
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.learningActivity.get());
        assertEquals(expectedActivity, firstWorker.getDailyActivity());
        assertEquals(expectedActivity, secondWorker.getDailyActivity());

        firstStation.clearResidents();
        secondStation.clearResidents();
        data.buildingsWork(null);

        assertEquals(0.0, transportCapacity(resources), EPSILON);
        assertEquals(0.0,
                data.getTransportState().getDailyReport().totalCapacity(), EPSILON);
        assertEquals(TownProductionStopReason.NO_ELIGIBLE_WORKERS,
                firstStation.getDailyReport().stopReason());
        assertEquals(TownProductionStopReason.NO_ELIGIBLE_WORKERS,
                secondStation.getDailyReport().stopReason());
    }

    @Test
    void unworkableStationResetsOldCapacityWithoutGrantingProficiency() {
        BlockPos pos = new BlockPos(1, 64, 1);
        Resident worker = standardWorker("Blocked", pos);
        TransportStationBuilding station = workableStation(pos, worker);
        station.setInitialized(false);
        TeamTownResourceHolder resources = new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 999.0));
        TeamTownData data = townData(resources, Map.of(pos, station), Map.of(worker.getUUID(), worker));
        double proficiencyBefore = worker.getWorkProficiency(TransportStationBuilding.class);

        data.buildingsWork(null);

        assertEquals(0.0, transportCapacity(resources), EPSILON);
        assertEquals(TownProductionStopReason.BUILDING_UNWORKABLE,
                station.getDailyReport().stopReason());
        assertEquals(proficiencyBefore,
                worker.getWorkProficiency(TransportStationBuilding.class), EPSILON);
        assertEquals(ResidentActivity.NONE, worker.getDailyActivity());
    }

    @Test
    void zeroOutputConfigurationDisablesProductionAndProficiencyGrowth() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        double previousOutput = config.transportCapacityPerStandardWorkerDay.get();
        BlockPos pos = new BlockPos(1, 64, 1);
        Resident worker = standardWorker("Disabled", pos);
        TransportStationBuilding station = workableStation(pos, worker);
        TeamTownResourceHolder resources = new TeamTownResourceHolder();
        TeamTownData data = townData(resources, Map.of(pos, station), Map.of(worker.getUUID(), worker));
        try {
            config.transportCapacityPerStandardWorkerDay.set(0.0);

            assertEquals(TownProductionStopReason.OUTPUT_DISABLED,
                    station.getForecast(data.createTeamTown()).stopReason());
            data.buildingsWork(null);

            assertEquals(0.0, transportCapacity(resources), EPSILON);
            assertEquals(1, station.getDailyReport().workerCount());
            assertEquals(0.0, station.getDailyReport().plannedCapacity(), EPSILON);
            assertEquals(TownProductionStopReason.OUTPUT_DISABLED,
                    station.getDailyReport().stopReason());
            assertEquals(0.0,
                    worker.getWorkProficiency(TransportStationBuilding.class), EPSILON);
            assertEquals(ResidentActivity.NONE, worker.getDailyActivity());
        } finally {
            config.transportCapacityPerStandardWorkerDay.set(previousOutput);
        }
    }

    @Test
    void repeatedEmptySettlementDoesNotCreateResourceOrAggregateSyncWork() {
        BlockPos pos = new BlockPos(1, 64, 1);
        TransportStationBuilding station = workableStation(pos, List.of());
        TeamTownData data = townData(
                new TeamTownResourceHolder(), Map.of(pos, station), Map.of());

        data.buildingsWork(null);
        data.getDataSyncCache().clearChanged();
        data.buildingsWork(null);

        assertFalse(data.getDataSyncCache().hasChangedResources());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());
        assertFalse(data.getDataSyncCache().hasChanges());
    }

    private static TeamTownData townData(
            TeamTownResourceHolder resources,
            Map<BlockPos, ITownBuilding> buildings,
            Map<UUID, Resident> residents
    ) {
        return new TeamTownData(
                "Transport Test",
                resources,
                buildings,
                residents,
                Map.of(),
                0,
                0,
                List.of(),
                TownStaffingPlan.EMPTY,
                -1L);
    }

    private static TransportStationBuilding workableStation(BlockPos pos, Resident resident) {
        return workableStation(pos, List.of(resident));
    }

    private static TransportStationBuilding workableStation(
            BlockPos pos,
            List<Resident> residents
    ) {
        return new TransportStationBuilding(
                pos,
                true,
                false,
                true,
                OccupiedVolume.EMPTY,
                residents.stream().map(Resident::getUUID).toList(),
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumFloorAreaBlocks.get(),
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumInteriorVolumeBlocks.get(),
                1);
    }

    private static Resident standardWorker(String name, BlockPos workPos) {
        return new Resident(
                name,
                "Worker",
                UUID.randomUUID(),
                50.0,
                50.0,
                50.0,
                50.0,
                0,
                Map.of(TransportStationBuilding.class.getSimpleName(), 0.0),
                Optional.of(new BlockPos(0, 64, 0)),
                Optional.of(workPos),
                Resident.AGE_ADULT,
                0);
    }

    private static double transportCapacity(TeamTownResourceHolder resources) {
        return resources.get(VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0));
    }
}
