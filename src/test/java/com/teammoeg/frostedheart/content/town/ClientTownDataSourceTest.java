/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientTownDataSourceTest {
    private CTeamDataManager previousServerManager;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // Initialize through the abstract base so its delegating Codec is
        // assigned only after the polymorphic interface dispatch completes.
        AbstractTownBuilding.CODEC.getClass();
    }

    @BeforeEach
    void useDedicatedClientTopology() {
        previousServerManager = CTeamDataManager.INSTANCE;
        CTeamDataManager.INSTANCE = null;
        CClientTeamDataManager.INSTANCE.reset();
    }

    @AfterEach
    void restoreGlobalManagers() {
        CClientTeamDataManager.INSTANCE.reset();
        CTeamDataManager.INSTANCE = previousServerManager;
    }

    @Test
    void resolvesAllTownBuildingTypesFromTheClientSnapshotWithoutAServerManager() {
        BlockPos housePos = new BlockPos(1, 64, 1);
        BlockPos mineBasePos = new BlockPos(2, 64, 1);
        BlockPos huntingPos = new BlockPos(3, 64, 1);
        BlockPos minePos = new BlockPos(4, 64, 1);
        BlockPos warehousePos = new BlockPos(5, 64, 1);

        WarehouseBuilding warehouse = new WarehouseBuilding(warehousePos);
        warehouse.setCapacity(22_000.0);

        Map<BlockPos, ITownBuilding> buildings = new LinkedHashMap<>();
        buildings.put(housePos, new HouseBuilding(housePos));
        buildings.put(mineBasePos, new MineBaseBuilding(mineBasePos));
        buildings.put(huntingPos, new HuntingBaseBuilding(huntingPos));
        buildings.put(minePos, new MineBuilding(minePos));
        buildings.put(warehousePos, warehouse);
        installClientSnapshot(buildings);

        TeamTown town = TeamTownData.getClientTown().orElseThrow();
        assertInstanceOf(HouseBuilding.class, town.getTownBuilding(housePos).orElseThrow());
        assertInstanceOf(MineBaseBuilding.class, town.getTownBuilding(mineBasePos).orElseThrow());
        assertInstanceOf(HuntingBaseBuilding.class, town.getTownBuilding(huntingPos).orElseThrow());
        assertInstanceOf(MineBuilding.class, town.getTownBuilding(minePos).orElseThrow());
        WarehouseBuilding syncedWarehouse = assertInstanceOf(
                WarehouseBuilding.class, town.getTownBuilding(warehousePos).orElseThrow());
        assertEquals(22_000.0, syncedWarehouse.getCapacity());
        assertTrue(syncedWarehouse.getCapacity() > Short.MAX_VALUE / 100.0);
    }

    @Test
    void resolvesTheReplacementSnapshotInsteadOfRetainingAStaleTownInstance() {
        BlockPos warehousePos = new BlockPos(8, 64, 8);
        WarehouseBuilding first = new WarehouseBuilding(warehousePos);
        first.setCapacity(22_000.0);
        installClientSnapshot(Map.of(warehousePos, first));

        TeamTown firstTown = TeamTownData.getClientTown().orElseThrow();

        WarehouseBuilding replacement = new WarehouseBuilding(warehousePos);
        replacement.setCapacity(44_000.0);
        installClientSnapshot(Map.of(warehousePos, replacement));

        TeamTown replacementTown = TeamTownData.getClientTown().orElseThrow();
        assertEquals(22_000.0, warehouse(firstTown, warehousePos).getCapacity());
        assertEquals(44_000.0, warehouse(replacementTown, warehousePos).getCapacity());
    }

    private static WarehouseBuilding warehouse(TeamTown town, BlockPos pos) {
        return assertInstanceOf(WarehouseBuilding.class, town.getTownBuilding(pos).orElseThrow());
    }

    private static void installClientSnapshot(Map<BlockPos, ITownBuilding> buildings) {
        TeamTownData snapshot = new TeamTownData(
                "Remote Town",
                new TeamTownResourceHolder(),
                buildings,
                Map.of(),
                Map.of(),
                0,
                0,
                List.of(),
                TownStaffingPlan.EMPTY,
                -1L);
        CClientTeamDataManager.INSTANCE.getInstance()
                .setData(FHSpecialDataTypes.TOWN_DATA, snapshot);
    }
}
