/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologyListener;
import com.teammoeg.frostedheart.content.town.transport.WarehouseTopologySnapshot;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTownWarehouseTopologyTest {
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
    void topologyFiltersInvalidWarehousesAndDetectsPositionChangesAtEqualTotalCapacity() {
        BlockPos firstPos = new BlockPos(0, 64, 0);
        BlockPos invalidPos = new BlockPos(4, 64, 0);
        BlockPos nonFinitePos = new BlockPos(6, 64, 0);
        WarehouseBuilding invalid = warehouse(invalidPos, 100.0, false);
        TeamTownData data = townData(Map.of(
                firstPos, warehouse(firstPos, 100.0, true),
                invalidPos, invalid,
                nonFinitePos, warehouse(nonFinitePos, Double.NaN, true)));
        TeamTown town = data.createTeamTown();

        WarehouseTopologySnapshot first = town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(List.of(firstPos), first.entries().stream()
                .map(entry -> entry.corePos()).toList());

        data.buildings.remove(firstPos);
        BlockPos movedPos = new BlockPos(20, 64, 0);
        data.buildings.put(movedPos, warehouse(movedPos, 100.0, true));
        data.markWarehouseTopologyDirty();
        WarehouseTopologySnapshot moved = town.prepareWarehouseTopology(Level.OVERWORLD);

        assertEquals(List.of(movedPos), moved.entries().stream()
                .map(entry -> entry.corePos()).toList());
        assertFalse(first.equals(moved));
        assertEquals(2, data.getWarehouseTopologyBuildCount());
    }

    @Test
    void listenerReplacementAndIdentitySafeUnregisterKeepTheCurrentInstance() {
        BlockPos warehousePos = new BlockPos(8, 64, 0);
        TeamTownData data = townData(Map.of(
                warehousePos, warehouse(warehousePos, 1_000.0, true)));
        TeamTown town = data.createTeamTown();
        GlobalPos devicePos = GlobalPos.of(Level.OVERWORLD, new BlockPos(2, 70, 0));
        AtomicInteger oldCalls = new AtomicInteger();
        AtomicInteger currentCalls = new AtomicInteger();
        WarehouseTopologyListener old = snapshot -> oldCalls.incrementAndGet();
        WarehouseTopologyListener current = snapshot -> currentCalls.incrementAndGet();

        town.registerWarehouseTopologyListener(devicePos, old);
        town.registerWarehouseTopologyListener(devicePos, current);
        town.unregisterWarehouseTopologyListener(devicePos, old);
        town.prepareWarehouseTopology(Level.OVERWORLD);

        assertEquals(0, oldCalls.get());
        assertEquals(1, currentCalls.get());
        town.unregisterWarehouseTopologyListener(devicePos, current);
        data.markWarehouseTopologyDirty();
        ((WarehouseBuilding) data.buildings.get(warehousePos)).setCapacity(2_000.0);
        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(1, currentCalls.get());
    }

    @Test
    void oneRefreshRecomputes4096EndpointsOnceAndIdlePrepareIsConstantTime() {
        BlockPos warehousePos = new BlockPos(8, 64, 0);
        TeamTownData data = townData(Map.of(
                warehousePos, warehouse(warehousePos, 1_000.0, true)));
        TeamTown town = data.createTeamTown();
        for (int index = 0; index < TownTransportSnapshot.MAX_RESERVATIONS; index++) {
            TransportEndpointId endpoint = endpoint(Level.OVERWORLD, index);
            data.getTransportState().replaceReservation(endpoint, new TransportReservation(
                    TransportEndpointKind.WAREHOUSE_INTERFACE,
                    20, 0.0, 20.0, TransportAdmissionStatus.ACTIVE));
        }
        data.getDataSyncCache().clearChanged();

        GlobalPos selfPos = GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 70, 0));
        AtomicInteger selfCalls = new AtomicInteger();
        AtomicInteger survivingCalls = new AtomicInteger();
        WarehouseTopologyListener[] selfRemoving = new WarehouseTopologyListener[1];
        selfRemoving[0] = snapshot -> {
            selfCalls.incrementAndGet();
            town.unregisterWarehouseTopologyListener(selfPos, selfRemoving[0]);
        };
        town.registerWarehouseTopologyListener(selfPos, selfRemoving[0]);
        town.registerWarehouseTopologyListener(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 70, 0)),
                snapshot -> { throw new IllegalStateException("expected test listener failure"); });
        town.registerWarehouseTopologyListener(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(2, 70, 0)),
                snapshot -> survivingCalls.incrementAndGet());

        town.prepareWarehouseTopology(Level.OVERWORLD);

        assertEquals(1, data.getWarehouseTopologyBuildCount());
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS,
                data.getWarehouseEndpointRecomputeCount());
        assertEquals(1, selfCalls.get());
        assertEquals(1, survivingCalls.get());
        assertEquals(2, data.getWarehouseListenerNotificationCount());
        assertTrue(data.getDataSyncCache().hasTransportStateChange());
        assertTrue(data.getTransportState().getReservedTransportCapacity() > 0.0);

        data.getDataSyncCache().clearChanged();
        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(1, data.getWarehouseTopologyBuildCount());
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS,
                data.getWarehouseEndpointRecomputeCount());
        assertEquals(1, survivingCalls.get());
        assertFalse(data.getDataSyncCache().hasTransportStateChange());

        WarehouseBuilding warehouse = (WarehouseBuilding) data.buildings.get(warehousePos);
        warehouse.setCapacity(2_000.0);
        data.markWarehouseTopologyDirty();
        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(2, data.getWarehouseTopologyBuildCount());
        assertEquals(2L * TownTransportSnapshot.MAX_RESERVATIONS,
                data.getWarehouseEndpointRecomputeCount());
        assertEquals(1, selfCalls.get());
        assertEquals(2, survivingCalls.get());
    }

    @Test
    void oneTopologyChangeNotifies4096LoadedDevicesOnceAndIdlePrepareDoesNothing() {
        BlockPos warehousePos = new BlockPos(8, 64, 0);
        TeamTownData data = townData(Map.of(
                warehousePos, warehouse(warehousePos, 1_000.0, true)));
        TeamTown town = data.createTeamTown();
        AtomicInteger callbacks = new AtomicInteger();
        for (int index = 0; index < TownTransportSnapshot.MAX_RESERVATIONS; index++) {
            town.registerWarehouseTopologyListener(
                    GlobalPos.of(Level.OVERWORLD, new BlockPos(index, 70, 0)),
                    snapshot -> callbacks.incrementAndGet());
        }

        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS, callbacks.get());
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS,
                data.getWarehouseListenerNotificationCount());

        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(TownTransportSnapshot.MAX_RESERVATIONS, callbacks.get());
        assertEquals(1, data.getWarehouseTopologyBuildCount());

        ((WarehouseBuilding) data.buildings.get(warehousePos)).setCapacity(2_000.0);
        data.markWarehouseTopologyDirty();
        town.prepareWarehouseTopology(Level.OVERWORLD);
        assertEquals(2L * TownTransportSnapshot.MAX_RESERVATIONS, callbacks.get());
        assertEquals(2L * TownTransportSnapshot.MAX_RESERVATIONS,
                data.getWarehouseListenerNotificationCount());
    }

    @Test
    void missingOrMismatchedTownDimensionMakesReservationsUnavailableWithoutDeletingRate() {
        BlockPos warehousePos = new BlockPos(8, 64, 0);
        TeamTownData data = townData(Map.of(
                warehousePos, warehouse(warehousePos, 1_000.0, true)));
        TransportEndpointId endpoint = endpoint(Level.NETHER, 0);
        data.getTransportState().replaceReservation(endpoint, new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                20, 1.0, 21.0, TransportAdmissionStatus.ACTIVE));
        TeamTown town = data.createTeamTown();

        town.prepareWarehouseTopology(Level.OVERWORLD);
        TransportReservation mismatch = town.getTransportReservation(endpoint).orElseThrow();
        assertEquals(TransportAdmissionStatus.UNAVAILABLE, mismatch.admissionStatus());
        assertEquals(20, mismatch.rateItemsPerSecond());
        assertEquals(0.0, mismatch.reservedTransportCapacity(), EPSILON);

        data.markWarehouseTopologyDirty();
        WarehouseTopologySnapshot missing = town.prepareWarehouseTopology(null);
        assertFalse(missing.isUsable());
        assertEquals(TransportAdmissionStatus.UNAVAILABLE,
                town.getTransportReservation(endpoint).orElseThrow().admissionStatus());
    }

    private static TeamTownData townData(Map<BlockPos, ? extends ITownBuilding> buildings) {
        Map<BlockPos, ITownBuilding> ordered = new LinkedHashMap<>();
        ordered.putAll(buildings);
        return new TeamTownData(
                "Topology Test",
                new TeamTownResourceHolder(Map.<ITownResourceKey, Double>of(
                        VirtualResourceType.TRANSPORT_CAPACITY.generateAttribute(0), 1_000_000.0)),
                ordered, Map.of(), Map.of(), 0, 0, List.of(),
                TownStaffingPlan.EMPTY, -1L);
    }

    private static WarehouseBuilding warehouse(BlockPos pos, double capacity, boolean workable) {
        return new WarehouseBuilding(pos, true, OccupiedVolume.EMPTY, workable,
                false, capacity, 1, 1, 0);
    }

    private static TransportEndpointId endpoint(net.minecraft.resources.ResourceKey<Level> dimension, int x) {
        return new TransportEndpointId(GlobalPos.of(dimension, new BlockPos(x, 64, 0)));
    }
}
