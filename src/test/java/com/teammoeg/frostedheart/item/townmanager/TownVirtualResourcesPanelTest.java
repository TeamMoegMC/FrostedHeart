/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointId;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownVirtualResourcesPanelTest {
    private static final String PREFIX = "gui.frostedheart.town_manager.virtual_resource.";

    @Test
    void deviceDetailsDefaultToCollapsedAndRemainExpandedAcrossSnapshotRefresh() {
        TownVirtualResourcesPanel.TransportDetailsState details =
                new TownVirtualResourcesPanel.TransportDetailsState();
        TownTransportSnapshot snapshot = snapshot(28.0);

        List<TownInfoPanel.Row> collapsed = TownVirtualResourcesPanel.transportRows(snapshot, details);
        assertFalse(details.expanded());
        assertTrue(indexOf(collapsed, PREFIX + "transport_daily_report")
                < indexOf(collapsed, PREFIX + "transport_details_expand"));
        assertEquals(-1, indexOf(collapsed, PREFIX + "transport_endpoint"));

        TownInfoPanel.Row expand = row(collapsed, PREFIX + "transport_details_expand");
        assertNotNull(expand.clickAction());
        expand.clickAction().run();
        assertTrue(details.expanded());

        List<TownInfoPanel.Row> expanded = TownVirtualResourcesPanel.transportRows(
                snapshot(14.0), details);
        int daily = indexOf(expanded, PREFIX + "transport_daily_report");
        int control = indexOf(expanded, PREFIX + "transport_details_collapse");
        int endpoint = indexOf(expanded, PREFIX + "transport_endpoint");
        assertTrue(daily < control && control < endpoint);

        List<TownInfoPanel.Row> endpointRows = expanded.stream()
                .filter(value -> key(value).equals(PREFIX + "transport_endpoint"))
                .toList();
        assertEquals(2, endpointRows.size());
        assertTrue(((String) contents(endpointRows.get(0)).getArgs()[1]).contains("1, 64, 0"));
        assertTrue(((String) contents(endpointRows.get(1)).getArgs()[1]).contains("5, 64, 0"));

        List<TownInfoPanel.Row> metrics = expanded.stream()
                .filter(value -> key(value).equals(PREFIX + "transport_endpoint_metrics"))
                .toList();
        assertEquals("1.4", contents(metrics.get(0)).getArgs()[0]);
        assertEquals("1.4", contents(metrics.get(1)).getArgs()[0]);
        assertEquals(PREFIX + "transport_admission.throttled",
                ((TranslatableContents) ((net.minecraft.network.chat.Component)
                        contents(metrics.get(0)).getArgs()[2]).getContents()).getKey());

        TownInfoPanel.Row collapse = row(expanded, PREFIX + "transport_details_collapse");
        collapse.clickAction().run();
        assertFalse(details.expanded());
    }

    private static TownTransportSnapshot snapshot(double totalCapacity) {
        return new TownTransportSnapshot(
                new TownTransportState.DailyReport(true, 64.0, 56.0),
                totalCapacity,
                List.of(entry(5), entry(1)));
    }

    private static TownTransportState.ReservationEntry entry(int x) {
        TransportEndpointId endpoint = new TransportEndpointId(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(x, 64, 0)));
        TransportReservation reservation = new TransportReservation(
                TransportEndpointKind.WAREHOUSE_INTERFACE,
                GlobalPos.of(Level.OVERWORLD, new BlockPos(100 + x, 64, 0)),
                20, 8.0, 28.0, TransportAdmissionStatus.ACTIVE);
        return new TownTransportState.ReservationEntry(endpoint, reservation);
    }

    private static TownInfoPanel.Row row(List<TownInfoPanel.Row> rows, String key) {
        return rows.stream().filter(value -> key(value).equals(key)).findFirst().orElseThrow();
    }

    private static int indexOf(List<TownInfoPanel.Row> rows, String key) {
        for (int index = 0; index < rows.size(); index++) {
            if (key(rows.get(index)).equals(key)) return index;
        }
        return -1;
    }

    private static String key(TownInfoPanel.Row row) {
        return row.text().getContents() instanceof TranslatableContents contents
                ? contents.getKey()
                : "";
    }

    private static TranslatableContents contents(TownInfoPanel.Row row) {
        return (TranslatableContents) row.text().getContents();
    }
}
