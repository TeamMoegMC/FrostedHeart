/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Current active warnings followed by the newest daily threshold crossings. */
public class TownEventsTab extends TownManagerTab {
    public TownEventsTab(TownManagerScreen screen) {
        super(screen);
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.BELL);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.events");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownInfoPanel(layer, TownManagerScreen.CONTENT_X, TownManagerScreen.CONTENT_Y,
                TownManagerScreen.CONTENT_WIDTH, TownManagerScreen.CONTENT_HEIGHT, this::rows));
    }

    private List<TownInfoPanel.Row> rows() {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(TownInfoPanel.Row.colored(Component.translatable(
                "gui.frostedheart.town_manager.active_warnings").withStyle(ChatFormatting.BOLD),
                0xFFFFAA00));
        TownOperationalStatus status = screen.getOperationalStatus();
        if (status == null) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.status_waiting"), 0xFFAAAAAA));
        } else if (status.activeAlerts().isEmpty()) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.status_normal"), 0xFF55FF55));
        } else {
            for (TownOperationalStatus.ActiveAlert alert : status.activeAlerts()) {
                rows.add(TownInfoPanel.Row.colored(
                        Component.literal("• ").append(TownSignalText.describe(
                                alert.type(), alert.affectedCount())),
                        TownSignalText.color(alert.severity())));
            }
        }
        rows.add(TownInfoPanel.Row.empty());
        rows.add(TownInfoPanel.Row.colored(Component.translatable(
                "gui.frostedheart.town_manager.recent_events").withStyle(ChatFormatting.BOLD),
                0xFFFFFFFF));

        List<TownSignalEvent> events = new ArrayList<>();
        if (screen.getTownData() != null) {
            for (TownHistoryEntry entry : screen.getTownData().getHistory()) events.addAll(entry.events());
        }
        events.sort(Comparator.comparingLong(TownSignalEvent::day).reversed()
                .thenComparing(Comparator.comparingInt(TownSignalEvent::hour).reversed()));
        if (events.isEmpty()) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.no_events"), 0xFF777777));
        } else {
            for (TownSignalEvent event : events) {
                Component time = Component.translatable("gui.frostedheart.town_manager.event_time",
                        event.day(), event.hour()).withStyle(ChatFormatting.DARK_GRAY);
                rows.add(TownInfoPanel.Row.colored(Component.empty().append(time).append(" ")
                                .append(TownSignalText.describe(event.type(), event.affectedCount())),
                        TownSignalText.color(event.severity())));
            }
        }
        return rows;
    }
}
