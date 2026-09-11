/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSnapshot;
import com.teammoeg.frostedheart.content.town.transport.device.P2PFilterSummaryState;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTransportSnapshotTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void codecCarriesWarehousePresentationMetadata() {
        TownTransportSnapshot snapshot = new TownTransportSnapshot(
                TownTransportState.DailyReport.EMPTY,
                128.0,
                4,
                0.075,
                List.of());

        var encoded = TownTransportSnapshot.CODEC
                .encodeStart(JsonOps.INSTANCE, snapshot).result().orElseThrow();
        assertEquals(snapshot, TownTransportSnapshot.CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
    }

    @Test
    void codecRejectsInvalidWarehousePresentationMetadata() {
        assertTrue(TownTransportSnapshot.CODEC.encodeStart(
                JsonOps.INSTANCE,
                new TownTransportSnapshot(
                        TownTransportState.DailyReport.EMPTY,
                        0.0,
                        -1,
                        0.05,
                        List.of())).error().isPresent());
        assertTrue(TownTransportSnapshot.CODEC.encodeStart(
                JsonOps.INSTANCE,
                new TownTransportSnapshot(
                        TownTransportState.DailyReport.EMPTY,
                        0.0,
                        0,
                        Double.NaN,
                        List.of())).error().isPresent());
    }

    @Test
    void codecCarriesBoundedP2PBindingsAndFilterSummaries() {
        P2PTerminalEndpoint sender = new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 1)),
                P2PTerminalRole.SHIPPING);
        P2PTerminalEndpoint receiver = new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(11, 64, 1)),
                P2PTerminalRole.RECEIVING);
        P2PBindingState bindings = P2PBindingState.EMPTY.apply(
                P2PBindingState.EMPTY.planConnection(sender, receiver, 20,
                        new java.util.UUID(0L, 7L)));
        P2PFilterSnapshot filteredStone = new P2PFilterSnapshot(
                true, false, List.of(new ItemStack(Items.STONE)));
        P2PFilterSummaryState filters = P2PFilterSummaryState.EMPTY.with(
                sender.pos(), filteredStone,
                new P2PFilterSnapshot(true, false, List.of()));
        TownTransportSnapshot snapshot = new TownTransportSnapshot(
                TownTransportState.DailyReport.EMPTY, 128.0, 4,
                0.075, 0.125, List.of(), bindings, filters);

        var encoded = TownTransportSnapshot.CODEC.encodeStart(JsonOps.INSTANCE, snapshot)
                .result().orElseThrow();
        TownTransportSnapshot decoded = TownTransportSnapshot.CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(bindings.bindings(), decoded.p2pBindingState().bindings());
        assertTrue(decoded.p2pFilterSummaryState().get(sender.pos()).isPresent());
        assertEquals(0.125, decoded.p2pDistanceCostPerBlock());
    }
}
