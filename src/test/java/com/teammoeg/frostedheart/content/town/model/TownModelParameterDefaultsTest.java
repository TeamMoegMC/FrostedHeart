/*
 * Copyright (c) 2026 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.town.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownModelParameterDefaultsTest {
    private static final double EPSILON = 1.0e-12;

    @Test
    void stageZeroConfigDeclarationsReferenceTownModelDefaults() throws IOException {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownModelParameters.GeneratorT1Parameters generator = parameters.generatorT1();
        String configSource = Files.readString(Path.of(
                "src/main/java/com/teammoeg/frostedheart/infrastructure/config/FHConfig.java"));
        String generatorSource = Files.readString(Path.of(
                "src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorData.java"));
        String climateEventsSource = Files.readString(Path.of(
                "src/main/java/com/teammoeg/frostedheart/content/climate/event/ClimateCommonEvents.java"));

        String defaultsSource = Files.readString(Path.of(
                "src/main/java/com/teammoeg/frostedheart/content/town/model/TownModelParameters.java"));
        String defaultsBody = defaultsSource.substring(
                defaultsSource.indexOf("public static final class Defaults"),
                defaultsSource.indexOf("public static final class GameUnits"));
        Matcher constants = Pattern.compile("public static final \\w+ ([A-Z0-9_]+) =")
                .matcher(defaultsBody);
        int constantCount = 0;
        while (constants.find()) {
            assertConfigUses(configSource, constants.group(1));
            constantCount++;
        }
        assertTrue(constantCount >= 70, "Expected the complete resident/housing/work default table.");

        assertTrue(generatorSource.contains("FHConfig.SERVER.TOWN.GENERATOR_T1"));
        assertTrue(generatorSource.contains("FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks"));
        assertTrue(climateEventsSource.contains("FHConfig.SERVER.TOWN.townUpdateIntervalGameTicks"));
        assertTrue(!generatorSource.contains("import com.teammoeg.frostedheart.content.town.model.TownModelParameters")
                        && !generatorSource.contains("TownModelParameters.currentDefaults()"),
                "Runtime generator code must read FHConfig, not source defaults directly.");
        for (Path runtimePath : List.of(
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/TeamTownData.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/building/ITownResidentWorkBuilding.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBuilding.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBlockEntity.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/mine/MineBaseBlockEntity.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/hunting/HuntingBaseBlockEntity.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/logistics/TransportStationBuilding.java"),
                Path.of("src/main/java/com/teammoeg/frostedheart/content/town/buildings/logistics/TransportStationBlockEntity.java"))) {
            assertRuntimeUsesConfig(runtimePath);
        }

        assertEquals(TownModelParameters.Defaults.GENERATOR_T1_BASE_FUEL_DURATION_MULTIPLIER,
                generator.baseFuelDurationMultiplier(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TOWN_UPDATE_INTERVAL_GAME_TICKS,
                generator.townBatchGameTicks());
        assertEquals(TownModelParameters.Defaults.HOUSING_MINIMUM_FLOOR_AREA_BLOCKS,
                parameters.housing().minimumFloorAreaBlocks());
        assertEquals(TownModelParameters.Defaults.RESIDENT_HOMELESS_HEALTH_LOSS_PER_DAY,
                parameters.residents().homelessHealthLossPerDay(), EPSILON);
        assertEquals(TownModelParameters.Defaults.MINING_FLOOR_BLOCKS_PER_WORKER_SLOT,
                parameters.mining().floorBlocksPerWorkerSlot(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_CAPACITY_PER_STANDARD_WORKER_DAY,
                parameters.transportStation().capacityPerStandardWorkerDay(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_FLOOR_BLOCKS_PER_WORKER_SLOT,
                parameters.transportStation().floorBlocksPerWorkerSlot(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_HEALTH_WEIGHT,
                parameters.transportStation().productivity().healthWeight(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_PHYSICAL_ACTIVITY,
                parameters.transportStation().activity().physical(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_STATION_LEARNING_ACTIVITY,
                parameters.transportStation().activity().learning(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TRANSPORT_CONSUMER_DEFAULT_RATE_ITEMS_PER_SECOND,
                parameters.transportConsumers().defaultRateItemsPerSecond());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_CONSUMER_MINIMUM_RATE_ITEMS_PER_SECOND,
                parameters.transportConsumers().minimumRateItemsPerSecond());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_CONSUMER_MAXIMUM_RATE_ITEMS_PER_SECOND,
                parameters.transportConsumers().maximumRateItemsPerSecond());
        assertEquals(TownModelParameters.Defaults.TRANSPORT_CONSUMER_WAREHOUSE_SCALE_COST_PER_METRIC,
                parameters.transportConsumers().warehouseScaleCostPerMetric(), EPSILON);
        assertEquals(1.0,
                parameters.transportStation().productivity().standardWorkerEquivalent(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TOWN_OBSERVATION_HISTORY_DAYS,
                parameters.observation().historyDays());
        assertEquals(90, parameters.observation().historyDays());
        assertEquals(TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_WARNING_DAYS,
                parameters.observation().reserveWarningDays(), EPSILON);
        assertEquals(TownModelParameters.Defaults.TOWN_OBSERVATION_RESERVE_CRITICAL_DAYS,
                parameters.observation().reserveCriticalDays(), EPSILON);
    }

    private static void assertConfigUses(String configSource, String constantName) {
        assertTrue(configSource.contains("TownModelParameters.Defaults." + constantName),
                "FHConfig must reference " + constantName);
    }

    private static void assertRuntimeUsesConfig(Path runtimePath) throws IOException {
        String source = Files.readString(runtimePath);
        assertTrue(source.contains("FHConfig.SERVER.TOWN"),
                runtimePath + " must read runtime values from FHConfig.");
        assertTrue(!source.contains("TownModelParameters.currentDefaults()")
                        && !source.contains("import com.teammoeg.frostedheart.content.town.model.TownModelParameters"),
                runtimePath + " must not read source defaults directly.");
    }
}
