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
 *
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalHistory;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 城镇近况页签：汇总城镇名称、人口、建筑、居民平均状态、
 * 无家可归/无工作人数，并给出相对前一日的变化。
 * <p>
 * Town overview tab: summarizes town name, population, buildings, average
 * resident stats, homeless/unemployed counts, and day-over-day changes.
 */
public class TownOverviewTab extends TownManagerTab {

    public TownOverviewTab(TownManagerScreen screen) {
        super(screen);
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(Items.COMPASS);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.town_manager.overview");
    }

    @Override
    public void build(UILayer layer) {
        layer.add(new TownInfoPanel(
                layer,
                TownManagerScreen.CONTENT_X,
                TownManagerScreen.CONTENT_Y + 16,
                TownManagerScreen.CONTENT_WIDTH,
                TownManagerScreen.CONTENT_HEIGHT - 16,
                this::collectRows));
        layer.add(new TownNameEditor(layer, screen::getTown));
    }

    private List<TownInfoPanel.Row> collectRows() {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        TeamTown town = screen.getTown();
        TeamTownData data = screen.getTownData();
        if (town == null || data == null) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town_manager.no_town"), 0xFFFF5555));
            return rows;
        }

        List<TownHistoryEntry> history = data.getHistory();
        TownHistoryEntry latest = history.isEmpty() ? null : history.get(history.size() - 1);
        TownHistoryEntry previous = history.size() >= 2 ? history.get(history.size() - 2) : null;
        TownOperationalStatus status = screen.getOperationalStatus();

        rows.add(TownInfoPanel.Row.text(separator()));

        rows.add(TownInfoPanel.Row.text(Component.translatable(
                "gui.frostedheart.town_manager.current_status").withStyle(ChatFormatting.YELLOW)));
        if (status == null) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.status_waiting"), 0xFFAAAAAA));
        } else if (status.activeAlerts().isEmpty()) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.status_normal"), 0xFF55FF55));
        } else {
            status.activeAlerts().forEach(alert -> rows.add(TownInfoPanel.Row.colored(
                    Component.literal("• ").append(TownSignalText.describe(alert.type(), alert.affectedCount())),
                    TownSignalText.color(alert.severity()))));
        }
        rows.add(TownInfoPanel.Row.text(separator()));

        int population = status == null ? town.getAllResidents().size() : status.population();
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.population")
                .append(value(String.valueOf(population)))
                .append(delta(previous == null || latest == null ? null : (double) (latest.population() - previous.population())))));

        rows.add(TownInfoPanel.Row.text(metricRow("gui.frostedheart.town_manager.food_reserve_days",
                status == null ? null : status.foodReserveDays(), reserveTrend(latest, previous, true))));
        if (status != null && status.tower().kind() == TownOperationalStatus.TowerKind.T2) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.t2_fuel_unavailable"), 0xFFFFAA00));
        } else {
            rows.add(TownInfoPanel.Row.text(metricRow("gui.frostedheart.town_manager.fuel_reserve_days",
                    status == null ? null : status.fuelReserveDays(), reserveTrend(latest, previous, false))));
        }

        if (status != null) {
            rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.tower_control")
                    .append(Component.translatable(status.tower().enabled()
                            ? "gui.frostedheart.town_manager.tower_enabled"
                            : "gui.frostedheart.town_manager.tower_disabled")
                            .withStyle(status.tower().enabled() ? ChatFormatting.GREEN : ChatFormatting.RED))));
            rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.tower_heat")
                    .append(Component.translatable(status.tower().active()
                            ? "gui.frostedheart.town_manager.tower_active"
                            : "gui.frostedheart.town_manager.tower_inactive")
                            .withStyle(status.tower().active() ? ChatFormatting.GREEN : ChatFormatting.RED))));
            rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.tower_mode")
                    .append(Component.translatable(status.tower().overdrive()
                            ? "gui.frostedheart.town_manager.tower_overdrive"
                            : "gui.frostedheart.town_manager.tower_normal")
                            .withStyle(status.tower().overdrive() ? ChatFormatting.GOLD : ChatFormatting.AQUA))));
            double damage = status.tower().overdriveFraction();
            String conditionKey = status.tower().broken() || damage >= 1.0
                    ? "gui.frostedheart.town_manager.tower_broken"
                    : damage <= 0.0
                    ? "gui.frostedheart.town_manager.tower_intact"
                    : status.tower().overdrive()
                    ? "gui.frostedheart.town_manager.tower_deteriorating"
                    : "gui.frostedheart.town_manager.tower_recovering";
            MutableComponent condition = label("gui.frostedheart.town_manager.tower_condition")
                    .append(Component.translatable(conditionKey)
                            .withStyle(status.tower().broken() ? ChatFormatting.RED
                                    : damage <= 0.0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            if (damage > 0.0) condition.append(Component.literal(" ").append(damageBar(damage)));
            rows.add(TownInfoPanel.Row.text(condition));
        }

        double avgHealth = status == null ? town.getAllResidents().stream()
                .mapToDouble(Resident::getHealth).average().orElse(0) : status.averageHealth();
        double avgMental = status == null ? town.getAllResidents().stream()
                .mapToDouble(Resident::getMental).average().orElse(0) : status.averageMental();
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.average_health")
                .append(statusBar(avgHealth))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.average_mental")
                .append(statusBar(avgMental))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.vulnerable_health")
                .append(status == null ? noData() : statusBar(status.p10Health()))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.vulnerable_mental")
                .append(status == null ? noData() : statusBar(status.p10Mental()))));

        long homeless = status == null ? town.getAllResidents().stream().filter(r -> r.getHousePos() == null).count()
                : status.homelessCount();
        long unemployed = status == null ? town.getAllResidents().stream().filter(r -> r.getWorkPos() == null).count()
                : status.unemployedCount();
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.unable_to_work")
                .append(coloredNumber(status == null ? 0 : status.unableToWorkCount(),
                        status == null || status.unableToWorkCount() == 0))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.exit_risk")
                .append(coloredNumber(status == null ? 0 : status.exitRiskCount(),
                        status == null || status.exitRiskCount() == 0))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.homeless")
                .append(coloredNumber(homeless, homeless == 0))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.unemployed")
                .append(coloredNumber(unemployed, unemployed == 0))));

        rows.add(TownInfoPanel.Row.text(separator()));
        rows.add(TownInfoPanel.Row.text(
                Component.translatable("gui.frostedheart.town_manager.days_recorded", history.size())
                        .withStyle(ChatFormatting.GRAY)));
        rows.add(TownInfoPanel.Row.text(
                Component.translatable("gui.frostedheart.town_manager.recorded_daily")
                        .withStyle(ChatFormatting.DARK_GRAY)));
        return rows;
    }

    private static Component metricRow(String key, TownOperationalStatus.Metric metric, Double trend) {
        MutableComponent row = label(key);
        if (metric == null || !metric.available()) {
            return row.append(Component.translatable("gui.frostedheart.town_manager.no_data")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        double warning = FHConfig.SERVER.TOWN.OBSERVATION.reserveWarningDays.get();
        double critical = FHConfig.SERVER.TOWN.OBSERVATION.reserveCriticalDays.get();
        ChatFormatting color = metric.value() < critical ? ChatFormatting.RED
                : metric.value() < warning ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        return row.append(Component.literal(formatOneDecimal(metric.value())).withStyle(color))
                .append(delta(trend));
    }

    private static Double reserveTrend(TownHistoryEntry latest, TownHistoryEntry previous, boolean food) {
        if (latest == null || previous == null) return null;
        TownOperationalHistory.Metric current = food ? latest.operational().foodReserveDays()
                : latest.operational().fuelReserveDays();
        TownOperationalHistory.Metric prior = food ? previous.operational().foodReserveDays()
                : previous.operational().fuelReserveDays();
        return current.available() && prior.available() ? current.value() - prior.value() : null;
    }

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": "));
    }

    private static MutableComponent value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.AQUA);
    }

    private static Component noData() {
        return Component.translatable("gui.frostedheart.town_manager.no_data")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent statusBar(double value) {
        double bounded = Math.max(0.0, Math.min(100.0, value));
        int filled = (int) Math.round(bounded / 10.0);
        ChatFormatting activeColor = bounded < 35.0 ? ChatFormatting.RED
                : bounded < 70.0 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        MutableComponent bar = Component.empty();
        for (int index = 0; index < 10; index++) {
            bar.append(Component.literal("■").withStyle(index < filled
                    ? activeColor : ChatFormatting.DARK_GRAY));
        }
        return bar;
    }

    private static MutableComponent damageBar(double fraction) {
        double bounded = Math.max(0.0, Math.min(1.0, fraction));
        int filled = (int) Math.round(bounded * 10.0);
        ChatFormatting activeColor = bounded < 0.35 ? ChatFormatting.GREEN
                : bounded < 0.7 ? ChatFormatting.GOLD : ChatFormatting.RED;
        MutableComponent bar = Component.empty();
        for (int index = 0; index < 10; index++) {
            bar.append(Component.literal("■").withStyle(index < filled
                    ? activeColor : ChatFormatting.DARK_GRAY));
        }
        return bar;
    }

    private static MutableComponent coloredNumber(long number, boolean good) {
        return Component.literal(String.valueOf(number))
                .withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /**
     * 格式化相对前一日的变化：上升绿色、下降红色、无变化或无数据为灰色。
     * <p>
     * Formats a day-over-day delta: green for increases, red for decreases,
     * gray for zero or unavailable.
     */
    private static Component delta(Double change) {
        if (change == null) {
            return Component.literal(" (-)").withStyle(ChatFormatting.DARK_GRAY);
        }
        String text = (change > 0 ? " (+" : " (") + formatNumber(change) + ")";
        ChatFormatting color = change > 0.05 ? ChatFormatting.GREEN
                : change < -0.05 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY;
        return Component.literal(text).withStyle(color);
    }

    /**
     * 整数数值不保留小数位，其余保留一位小数。
     * <p>
     * Whole numbers render without decimals; others keep one decimal place.
     */
    private static String formatNumber(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1e9) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatOneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component separator() {
        return Component.literal("───────────────").withStyle(ChatFormatting.DARK_GRAY);
    }
}
