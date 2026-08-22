/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSummary;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationDecision;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseInterfaceTransportViewTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void viewCarriesServerDerivedReservationAndTownValuesThroughCodec() {
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)),
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        WarehouseInterfaceTransportView view = WarehouseInterfaceTransportView.from(
                WarehouseInterfaceBlockEntity.STATUS_WORKING,
                Optional.of(reservation),
                new TownTransportSummary(14.0, 28.0, 0.0, 14.0, 0.5),
                TransportReservationDecision.INSUFFICIENT_CAPACITY,
                640);

        var encoded = WarehouseInterfaceTransportView.CODEC.encodeStart(JsonOps.INSTANCE, view).result().orElseThrow();
        WarehouseInterfaceTransportView decoded = WarehouseInterfaceTransportView.CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(view, decoded);
        assertEquals(WarehouseInterfaceTransportStatus.THROTTLED, decoded.status());
        assertEquals(20, decoded.rateItemsPerSecond());
        assertEquals(640, decoded.maximumRateItemsPerSecond());
        assertEquals(10.0, decoded.effectiveRateItemsPerSecond(), 1.0e-9);
        assertEquals("20", WarehouseInterfaceScreen.rateInputText(decoded));
        assertTrue(decoded.isRateLimited());
    }

    @Test
    void codecRejectsNonFiniteOrInconsistentClientViews() {
        WarehouseInterfaceTransportView invalid = new WarehouseInterfaceTransportView(
                WarehouseInterfaceTransportStatus.ACTIVE,
                TransportReservationDecision.ACCEPTED,
                20, 1280, Double.NaN, 0.0, 0.0, 0.0);

        assertTrue(WarehouseInterfaceTransportView.CODEC
                .encodeStart(JsonOps.INSTANCE, invalid).error().isPresent());
    }

    @Test
    void codecRetainsAnAcceptedRateAboveALaterLoweredSelectableMaximum() {
        WarehouseInterfaceTransportView view = new WarehouseInterfaceTransportView(
                WarehouseInterfaceTransportStatus.ACTIVE,
                TransportReservationDecision.ACCEPTED,
                1280, 640, 1280.0, 1280.0, 2000.0, 720.0);

        var encoded = WarehouseInterfaceTransportView.CODEC
                .encodeStart(JsonOps.INSTANCE, view).result().orElseThrow();
        assertEquals(view, WarehouseInterfaceTransportView.CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
        assertEquals(640, WarehouseInterfaceScreen.rateForScroll("1280", 1280, 640));
    }

    @Test
    void activeReservationBecomesThrottledWhenTownSupplyFalls() {
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)),
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);

        WarehouseInterfaceTransportView view = WarehouseInterfaceTransportView.from(
                WarehouseInterfaceBlockEntity.STATUS_WORKING,
                Optional.of(reservation),
                new TownTransportSummary(14.0, 28.0, 0.0, 14.0, 0.5),
                TransportReservationDecision.ACCEPTED);

        assertEquals(WarehouseInterfaceTransportStatus.THROTTLED, view.status());
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.SHORTAGE,
                WarehouseInterfaceBlock.TransportVisualState.from(view.status()));
        assertEquals(10.0, view.effectiveRateItemsPerSecond(), 1.0e-9);
    }

    @Test
    void restoredSupplyReturnsToTheAcceptedRateWithoutChangingTheInputValue() {
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)),
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);

        WarehouseInterfaceTransportView view = WarehouseInterfaceTransportView.from(
                WarehouseInterfaceBlockEntity.STATUS_WORKING,
                Optional.of(reservation),
                new TownTransportSummary(28.0, 28.0, 0.0, 0.0, 1.0),
                TransportReservationDecision.INSUFFICIENT_CAPACITY);

        assertEquals(WarehouseInterfaceTransportStatus.ACTIVE, view.status());
        assertEquals(20, view.rateItemsPerSecond());
        assertEquals(20.0, view.effectiveRateItemsPerSecond(), 1.0e-9);
        assertEquals("20", WarehouseInterfaceScreen.rateInputText(view));
        assertFalse(view.isRateLimited());
    }

    @Test
    void rateInputScrollMatchesTargetAmountModifiersAndClampsToServerMaximum() {
        assertEquals(1280, WarehouseInterfaceTransportView.EMPTY.maximumRateItemsPerSecond());
        assertEquals(1, WarehouseInterfaceScreen.rateScrollIncrement(false, false));
        assertEquals(8, WarehouseInterfaceScreen.rateScrollIncrement(true, false));
        assertEquals(16, WarehouseInterfaceScreen.rateScrollIncrement(false, true));
        assertEquals(64, WarehouseInterfaceScreen.rateScrollIncrement(true, true));

        assertEquals(21, WarehouseInterfaceScreen.adjustRateForScroll(
                20, 1, false, false, 1280));
        assertEquals(12, WarehouseInterfaceScreen.adjustRateForScroll(
                20, -1, true, false, 1280));
        assertEquals(1280, WarehouseInterfaceScreen.adjustRateForScroll(
                1270, 1, false, true, 1280));
        assertEquals(0, WarehouseInterfaceScreen.adjustRateForScroll(
                20, -1, true, true, 1280));
        assertEquals(20, WarehouseInterfaceScreen.rateForScroll("-1", 20, 1280));
        assertTrue(WarehouseInterfaceScreen.exceedsMaximumRate("1281", 1280));
        assertFalse(WarehouseInterfaceScreen.exceedsMaximumRate("1280", 1280));
    }

    @Test
    void compactLayoutKeepsLogicalAndRenderedSlotBandsAligned() {
        assertEquals(218, WarehouseInterfaceMenu.SCREEN_HEIGHT);
        assertEquals(20, WarehouseInterfaceMenu.INTERFACE_SLOT_Y
                - WarehouseInterfaceMenu.TARGET_FILTER_Y);
        assertEquals(58, WarehouseInterfaceMenu.PLAYER_HOTBAR_Y
                - WarehouseInterfaceMenu.PLAYER_INVENTORY_Y);
    }

    @Test
    void blockVisualStateHasStableFiniteMapping() {
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.ACTIVE,
                WarehouseInterfaceBlock.TransportVisualState.from(WarehouseInterfaceTransportStatus.ACTIVE));
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.DISABLED,
                WarehouseInterfaceBlock.TransportVisualState.from(WarehouseInterfaceTransportStatus.DISABLED));
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.SHORTAGE,
                WarehouseInterfaceBlock.TransportVisualState.from(
                        WarehouseInterfaceTransportStatus.THROTTLED));
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.UNAVAILABLE,
                WarehouseInterfaceBlock.TransportVisualState.from(WarehouseInterfaceTransportStatus.UNBOUND));
        assertEquals(WarehouseInterfaceBlock.TransportVisualState.UNAVAILABLE,
                WarehouseInterfaceBlock.TransportVisualState.from(
                WarehouseInterfaceTransportStatus.WAREHOUSE_UNAVAILABLE));
    }

    @Test
    void blockStateIsWrittenOnlyForANetVisualChange() {
        assertFalse(WarehouseInterfaceBlock.shouldUpdateTransportVisualState(
                WarehouseInterfaceBlock.TransportVisualState.ACTIVE,
                WarehouseInterfaceBlock.TransportVisualState.ACTIVE));
        assertTrue(WarehouseInterfaceBlock.shouldUpdateTransportVisualState(
                WarehouseInterfaceBlock.TransportVisualState.ACTIVE,
                WarehouseInterfaceBlock.TransportVisualState.SHORTAGE));
    }
}
