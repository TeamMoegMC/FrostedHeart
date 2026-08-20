/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransportStationConfigTest {
    @BeforeAll
    static void loadServerConfigDefaults() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void spaceSettingsUseTownModelDefaultsAsTheirSingleSource() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;

        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_FLOOR_BLOCKS_PER_WORKER_SLOT,
                config.floorBlocksPerWorkerSlot.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MINIMUM_WORKER_SLOTS,
                config.minimumWorkerSlots.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MINIMUM_FLOOR_AREA_BLOCKS,
                config.minimumFloorAreaBlocks.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MINIMUM_INTERIOR_VOLUME_BLOCKS,
                config.minimumInteriorVolumeBlocks.get());
    }

    @Test
    void productionSettingsUseTownModelDefaultsAsTheirSingleSource() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;

        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_CAPACITY_PER_STANDARD_WORKER_DAY,
                config.transportCapacityPerStandardWorkerDay.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_ZERO,
                config.productivityAtAttributeZero.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_PRODUCTIVITY_AT_ATTRIBUTE_HUNDRED,
                config.productivityAtAttributeHundred.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MAXIMUM_PROFICIENCY,
                config.maximumProficiency.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_BONUS_AT_MAXIMUM_PROFICIENCY,
                config.bonusAtMaximumProficiency.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MINIMUM_PRODUCTIVITY,
                config.minimumResidentProductivity.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MAXIMUM_PRODUCTIVITY,
                config.maximumResidentProductivity.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_HEALTH_WEIGHT,
                config.healthWeight.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_MENTAL_WEIGHT,
                config.mentalWeight.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_STRENGTH_WEIGHT,
                config.strengthWeight.get());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_INTELLIGENCE_WEIGHT,
                config.intelligenceWeight.get());
    }
}
