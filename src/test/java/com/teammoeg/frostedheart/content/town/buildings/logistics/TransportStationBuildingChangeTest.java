/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportStationBuildingChangeTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void settersOnlyFireForNetChanges() {
        TransportStationBuilding building = new TransportStationBuilding(BlockPos.ZERO);
        AtomicInteger changes = new AtomicInteger();
        building.setChangeEventListener(event -> changes.incrementAndGet());

        building.setArea(24);
        building.setArea(24);
        building.setVolume(72);
        building.setVolume(72);
        building.setMaxResidents(6);
        building.setMaxResidents(6);

        assertEquals(3, changes.get());
        assertEquals(0.0, building.getResidentScore(null));
    }

    @Test
    void identicalPeriodicScanResultsDoNotFireAdditionalChanges() {
        TransportStationBuilding building = new TransportStationBuilding(BlockPos.ZERO);
        AtomicInteger changes = new AtomicInteger();
        building.setChangeEventListener(event -> changes.incrementAndGet());
        OccupiedVolume firstVolume = occupiedVolume();

        TransportStationBlockEntity.applyScanResult(building, 24, 72, firstVolume);
        int firstScanChanges = changes.get();
        TransportStationBlockEntity.applyScanResult(
                building, 24, 72, occupiedVolume());

        assertTrue(firstScanChanges > 0);
        assertEquals(firstScanChanges, changes.get());
    }

    @Test
    void sharedMaxResidentsSetterOnlyFiresForNetChanges() {
        TransportStationBuilding building = new TransportStationBuilding(BlockPos.ZERO);
        AtomicInteger changes = new AtomicInteger();
        building.setChangeEventListener(event -> changes.incrementAndGet());

        building.setMaxResidents(4);
        building.setMaxResidents(4);

        assertEquals(1, changes.get());
    }

    @Test
    void structurallyValidStationStillRequiresMinimumAreaAndVolume() {
        TransportStationBuilding building = new TransportStationBuilding(BlockPos.ZERO);
        building.setInitialized(true);
        building.setIsStructureValid(true);
        int minimumArea = FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumFloorAreaBlocks.get();
        int minimumVolume = FHConfig.SERVER.TOWN.TRANSPORT_STATION.minimumInteriorVolumeBlocks.get();

        building.setArea(minimumArea - 1);
        building.setVolume(minimumVolume);
        assertFalse(building.isBuildingWorkable());

        building.setArea(minimumArea);
        building.setVolume(minimumVolume - 1);
        assertFalse(building.isBuildingWorkable());

        building.setVolume(minimumVolume);
        assertTrue(building.isBuildingWorkable());
    }

    private static OccupiedVolume occupiedVolume() {
        OccupiedVolume volume = new OccupiedVolume();
        volume.add(new BlockPos(1, 64, 1));
        volume.add(new BlockPos(2, 64, 1));
        return volume;
    }
}
