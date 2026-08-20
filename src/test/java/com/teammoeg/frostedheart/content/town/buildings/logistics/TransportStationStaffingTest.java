/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.model.TownAssignmentModel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TransportStationStaffingTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void stationJoinsStaffingPlanAndTargetIsBoundedByCapacity() {
        TransportStationBuilding station = station(BlockPos.ZERO, 3, true);
        Map<BlockPos, AbstractTownBuilding> buildings = Map.of(station.getPos(), station);

        TownStaffingPlan normalized = TownStaffingPlan.EMPTY.normalize(buildings);
        TownStaffingPlan aboveCapacity = normalized.withTarget(
                station.getPos(), 99, buildings).orElseThrow();
        TownStaffingPlan belowZero = aboveCapacity.withTarget(
                station.getPos(), -99, buildings).orElseThrow();

        assertEquals(List.of(station.getPos()), normalized.entries().stream()
                .map(TownStaffingPlan.Entry::building).toList());
        assertEquals(3, aboveCapacity.target(station.getPos()));
        assertEquals(0, belowZero.target(station.getPos()));
    }

    @Test
    void unworkableStationContributesNoAssignmentCapacity() {
        TransportStationBuilding unworkable = station(new BlockPos(1, 64, 0), 1, false);
        TransportStationBuilding workable = station(new BlockPos(2, 64, 0), 1, true);
        TownAssignmentModel.Plan<String, TransportStationBuilding> plan =
                TownAssignmentModel.plan(
                        List.of("first", "second"),
                        List.of(workplace(unworkable), workplace(workable)),
                        ignored -> null,
                        (station, resident) -> true,
                        (station, resident) -> station.getResidentScore(null),
                        Comparator.naturalOrder());

        assertEquals(0, plan.workplaces().get(unworkable).capacity());
        assertEquals(1, plan.workplaces().get(workable).capacity());
        assertEquals(1, plan.assignments().size());
        assertSame(workable, plan.assignments().get(0).workplace());
        assertEquals(List.of("second"), plan.unassignedResidents());
    }

    private static TownAssignmentModel.Workplace<TransportStationBuilding> workplace(
            TransportStationBuilding station
    ) {
        return new TownAssignmentModel.Workplace<>(
                station,
                station.getMaxResidents(),
                station.getMaxResidents(),
                station.isBuildingWorkable());
    }

    private static TransportStationBuilding station(
            BlockPos pos,
            int capacity,
            boolean workable
    ) {
        TransportStationBuilding station = new TransportStationBuilding(pos);
        station.setMaxResidents(capacity);
        station.setInitialized(true);
        station.setIsStructureValid(true);
        int minimumArea = FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumFloorAreaBlocks.get();
        int minimumVolume = FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumInteriorVolumeBlocks.get();
        station.setArea(workable ? minimumArea : minimumArea - 1);
        station.setVolume(minimumVolume);
        return station;
    }
}
