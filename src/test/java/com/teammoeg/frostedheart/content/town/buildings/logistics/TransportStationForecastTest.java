/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportStationForecastTest {
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
    void forecastUsesTheSameEligibleWorkersAndFormulaAsSettlement() {
        BlockPos pos = new BlockPos(4, 64, 4);
        Resident worker = worker(pos);
        TransportStationBuilding station = workableStation(pos, List.of(worker.getUUID()));
        TeamTown town = town(pos, station, Map.of(worker.getUUID(), worker));

        TransportStationBuilding.TransportStationForecast forecast = station.getForecast(town);

        assertEquals(1, forecast.workerCount());
        assertEquals(1.0, forecast.totalProductivity(), EPSILON);
        assertEquals(64.0, forecast.plannedCapacity(), EPSILON);
        assertEquals(TownProductionStopReason.NONE, forecast.stopReason());
    }

    @Test
    void forecastExplainsNoWorkersAndUnworkableStations() {
        BlockPos pos = new BlockPos(4, 64, 4);
        TransportStationBuilding station = workableStation(pos, List.of());
        TeamTown emptyTown = town(pos, station, Map.of());

        assertEquals(TownProductionStopReason.NO_ELIGIBLE_WORKERS,
                station.getForecast(emptyTown).stopReason());

        station.setInitialized(false);
        assertEquals(TownProductionStopReason.BUILDING_UNWORKABLE,
                station.getForecast(emptyTown).stopReason());
    }

    @Test
    void residentsInitializeTransportProficiencyForPredictionAndWork() {
        Resident resident = worker(BlockPos.ZERO);

        assertTrue(resident.getWorkProficiency().containsKey(
                TransportStationBuilding.class.getSimpleName()));
    }

    private static TeamTown town(
            BlockPos pos,
            TransportStationBuilding station,
            Map<UUID, Resident> residents
    ) {
        return new TeamTownData(
                "Forecast Test",
                new TeamTownResourceHolder(),
                Map.<BlockPos, ITownBuilding>of(pos, station),
                residents,
                Map.of(),
                0,
                0,
                List.of(),
                TownStaffingPlan.EMPTY,
                -1L).createTeamTown();
    }

    private static TransportStationBuilding workableStation(BlockPos pos, List<UUID> residents) {
        return new TransportStationBuilding(
                pos,
                true,
                false,
                true,
                OccupiedVolume.EMPTY,
                residents,
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumFloorAreaBlocks.get(),
                FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumInteriorVolumeBlocks.get(),
                1);
    }

    private static Resident worker(BlockPos workPos) {
        return new Resident(
                "Forecast",
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
}
