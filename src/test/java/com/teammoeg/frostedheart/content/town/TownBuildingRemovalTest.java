/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.block.TownBlockEntity;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionType;
import com.teammoeg.frostedheart.content.town.resource.action.TownResourceActions;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownBuildingRemovalTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
        // Initialize through the abstract base after every concrete CODEC has
        // been assigned, avoiding the dispatch builder's circular init path.
        AbstractTownBuilding.CODEC.getClass();
    }

    @Test
    void removingTransportStationClearsResidentJobsAndDetachedRoster() {
        BlockPos stationPos = new BlockPos(7, 64, 7);
        TransportStationBuilding station = new TransportStationBuilding(stationPos);
        Resident rostered = resident("Station Rostered");
        Resident inconsistent = resident("Station Inconsistent");
        station.addResident(rostered);
        inconsistent.setWorkPos(stationPos);
        TeamTown town = town(
                Map.of(stationPos, station),
                Map.of(rostered.getUUID(), rostered, inconsistent.getUUID(), inconsistent),
                new TeamTownResourceHolder());

        town.removeTownBlock(null, stationPos);

        assertTrue(town.getTownBuilding(stationPos).isEmpty());
        assertTrue(town.getResident(rostered.getUUID()).isPresent());
        assertTrue(town.getResident(inconsistent.getUUID()).isPresent());
        assertNull(rostered.getWorkPos());
        assertNull(inconsistent.getWorkPos());
        assertTrue(station.getResidentsID().isEmpty());
    }

    @Test
    void removalKeepsResidentsButClearsRosterAndDefensivePositionReferences() {
        BlockPos workPos = new BlockPos(1, 64, 1);
        BlockPos spareWorkPos = new BlockPos(2, 64, 1);
        BlockPos housePos = new BlockPos(3, 64, 1);
        MineBaseBuilding work = new MineBaseBuilding(workPos);
        MineBaseBuilding spareWork = new MineBaseBuilding(spareWorkPos);
        HouseBuilding house = new HouseBuilding(housePos);
        Resident rostered = resident("Rostered");
        Resident inconsistent = resident("Inconsistent");
        work.addResident(rostered);
        house.addResident(rostered);
        inconsistent.setWorkPos(workPos);
        inconsistent.setHousePos(housePos);

        TeamTown town = town(
                Map.of(workPos, work, spareWorkPos, spareWork, housePos, house),
                Map.of(rostered.getUUID(), rostered, inconsistent.getUUID(), inconsistent),
                new TeamTownResourceHolder());

        town.removeTownBlock(null, workPos);

        assertTrue(town.getResident(rostered.getUUID()).isPresent());
        assertNull(rostered.getWorkPos(), "a spare workplace must not trigger midday reassignment");
        assertNull(inconsistent.getWorkPos());
        assertEquals(housePos, inconsistent.getHousePos());

        town.removeTownBlock(null, housePos);

        assertNull(rostered.getHousePos());
        assertNull(inconsistent.getHousePos());
        assertTrue(town.getResident(rostered.getUUID()).isPresent());
        assertTrue(town.getResident(inconsistent.getUUID()).isPresent());
    }

    @Test
    void removingMinePrunesBaseLinkAndRepeatedRemovalIsSafe() {
        BlockPos basePos = new BlockPos(10, 64, 10);
        BlockPos minePos = new BlockPos(20, 32, 20);
        MineBaseBuilding base = new MineBaseBuilding(basePos);
        MineBuilding mine = new MineBuilding(minePos);
        base.addLinkedMine(minePos);
        TeamTown town = town(Map.of(basePos, base, minePos, mine), Map.of(), new TeamTownResourceHolder());

        town.removeTownBlock(null, minePos);

        assertFalse(base.getLinkedMines().contains(minePos));
        assertTrue(town.getTownBuilding(minePos).isEmpty());
        assertDoesNotThrow(() -> town.removeTownBlock(null, minePos));
    }

    @Test
    void warehouseRemovalDropsOnlyDerivedCapacityAndKeepsOverCapacityStockConsumable() {
        BlockPos largePos = new BlockPos(30, 64, 30);
        BlockPos smallPos = new BlockPos(40, 64, 40);
        WarehouseBuilding large = workableWarehouse(largePos, 100.0);
        WarehouseBuilding small = workableWarehouse(smallPos, 50.0);
        TeamTownResourceHolder resources = new TeamTownResourceHolder();
        TeamTown town = town(Map.of(largePos, large, smallPos, small), Map.of(), resources);
        TeamTownData data = town.getTownData().orElseThrow();
        data.reloadMaxCapacity();
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        town.getActionExecutorHandler().execute(new TownResourceActions.ItemResourceAction(
                cobblestone, ResourceActionType.ADD, 120.0, ResourceActionMode.ATTEMPT));
        double occupiedBefore = resources.getOccupiedCapacity();

        town.removeTownBlock(null, largePos);

        assertEquals(120.0, resources.get(cobblestone), 1.0e-9);
        assertEquals(occupiedBefore, resources.getOccupiedCapacity(), 1.0e-9);
        assertEquals(50.0, resources.get(
                VirtualResourceType.MAX_CAPACITY.generateAttribute(0)), 1.0e-9);
        var rejectedAdd = town.getActionExecutorHandler().execute(
                new TownResourceActions.ItemResourceAction(
                        cobblestone, ResourceActionType.ADD, 1.0, ResourceActionMode.MAXIMIZE));
        assertEquals(0.0, rejectedAdd.modifiedAmount(), 1.0e-9);

        var consumed = town.getActionExecutorHandler().execute(
                new TownResourceActions.ItemResourceAction(
                        cobblestone, ResourceActionType.COST, 20.0, ResourceActionMode.ATTEMPT));
        assertEquals(20.0, consumed.modifiedAmount(), 1.0e-9);
        assertEquals(100.0, resources.get(cobblestone), 1.0e-9);

        town.removeTownBlock(null, smallPos);

        assertEquals(0.0, resources.get(
                VirtualResourceType.MAX_CAPACITY.generateAttribute(0)), 1.0e-9);
        assertEquals(100.0, resources.get(cobblestone), 1.0e-9);
    }

    @Test
    void removalRecalculatesSurvivingOverlapFlags() {
        BlockPos firstPos = new BlockPos(50, 64, 50);
        BlockPos secondPos = new BlockPos(51, 64, 50);
        MineBuilding first = new MineBuilding(firstPos);
        MineBuilding second = new MineBuilding(secondPos);
        OccupiedVolume sharedFirst = new OccupiedVolume();
        OccupiedVolume sharedSecond = new OccupiedVolume();
        BlockPos shared = new BlockPos(55, 64, 55);
        sharedFirst.add(shared);
        sharedSecond.add(shared);
        first.setOccupiedVolume(sharedFirst);
        second.setOccupiedVolume(sharedSecond);
        TeamTown town = town(Map.of(firstPos, first, secondPos, second), Map.of(), new TeamTownResourceHolder());
        TeamTownData data = town.getTownData().orElseThrow();
        data.checkOccupiedAreaOverlap();
        assertTrue(first.isOccupiedAreaOverlapped());
        assertTrue(second.isOccupiedAreaOverlapped());

        town.removeTownBlock(null, firstPos);

        assertFalse(second.isOccupiedAreaOverlapped());
    }

    @Test
    void samePositionReplacementRunsOldBuildingCleanupBeforeRegistration() {
        BlockPos pos = new BlockPos(60, 64, 60);
        HouseBuilding house = new HouseBuilding(pos);
        Resident resident = resident("Replacement");
        house.addResident(resident);
        TeamTown town = town(Map.of(pos, house), Map.of(resident.getUUID(), resident), new TeamTownResourceHolder());
        MineBuilding replacement = new MineBuilding(pos);

        town.addTownBlock(pos, new FixedTownBlockEntity(replacement));

        assertNull(resident.getHousePos());
        assertSame(replacement, town.getTownBuilding(pos).orElseThrow());
        assertInstanceOf(MineBuilding.class, town.getTownBuilding(pos).orElseThrow());
    }

    private static WarehouseBuilding workableWarehouse(BlockPos pos, double capacity) {
        WarehouseBuilding warehouse = new WarehouseBuilding(pos);
        warehouse.setInitialized(true);
        warehouse.setIsStructureValid(true);
        warehouse.setCapacity(capacity);
        return warehouse;
    }

    private static Resident resident(String firstName) {
        return new Resident(
                firstName,
                "Resident",
                UUID.randomUUID(),
                50.0,
                50.0,
                50.0,
                50.0,
                0,
                Map.of(
                        HuntingBaseBuilding.class.getSimpleName(), 0.0,
                        MineBaseBuilding.class.getSimpleName(), 0.0),
                Optional.empty(),
                Optional.empty(),
                Resident.AGE_ADULT,
                0);
    }

    private static TeamTown town(
            Map<BlockPos, ? extends ITownBuilding> buildings,
            Map<UUID, Resident> residents,
            TeamTownResourceHolder resources
    ) {
        Map<BlockPos, ITownBuilding> orderedBuildings = new LinkedHashMap<>();
        orderedBuildings.putAll(buildings);
        return new TeamTownData(
                "Removal Test",
                resources,
                orderedBuildings,
                residents,
                Map.of(),
                0,
                0,
                List.of(),
                TownStaffingPlan.EMPTY,
                -1L).createTeamTown();
    }

    private record FixedTownBlockEntity(MineBuilding building) implements TownBlockEntity<MineBuilding> {
        @Override
        public void refresh(@NotNull MineBuilding building) {
        }

        @Override
        public Optional<MineBuilding> getBuilding() {
            return Optional.of(building);
        }

        @Override
        public @Nullable MineBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
            return abstractTownBuilding instanceof MineBuilding mine ? mine : null;
        }

        @Override
        public @NotNull MineBuilding createBuilding() {
            return building;
        }
    }
}
