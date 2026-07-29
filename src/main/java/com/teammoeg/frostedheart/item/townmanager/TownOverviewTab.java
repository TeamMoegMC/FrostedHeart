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
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
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
        return CIcons.getIcon(Items.BELL);
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
                TownManagerScreen.CONTENT_Y,
                TownManagerScreen.CONTENT_WIDTH,
                TownManagerScreen.CONTENT_HEIGHT,
                this::collectRows));
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

        rows.add(TownInfoPanel.Row.colored(
                Component.literal(town.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                0xFFFFAA00));
        rows.add(TownInfoPanel.Row.text(separator()));

        int population = town.getAllResidents().size();
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.population")
                .append(value(String.valueOf(population)))
                .append(delta(previous == null || latest == null ? null : (double) (latest.population() - previous.population())))));

        int buildingCount = town.getTownBuildings().size();
        long structureValid = town.getTownBuildings().values().stream()
                .filter(AbstractTownBuilding::isStructureValid).count();
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.building_count")
                .append(value(String.valueOf(buildingCount)))
                .append(Component.literal(" ")
                        .append(Component.translatable("gui.frostedheart.town_manager.structure_count", structureValid)
                                .withStyle(ChatFormatting.DARK_GRAY)))));

        double avgHealth = town.getAllResidents().stream().mapToDouble(Resident::getHealth).average().orElse(0);
        double avgMental = town.getAllResidents().stream().mapToDouble(Resident::getMental).average().orElse(0);
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.avg_health")
                .append(value(formatOneDecimal(avgHealth) + " / 100"))
                .append(delta(previous == null || latest == null ? null : latest.avgHealth() - previous.avgHealth()))));
        rows.add(TownInfoPanel.Row.text(label("gui.frostedheart.town_manager.avg_mental")
                .append(value(formatOneDecimal(avgMental) + " / 100"))
                .append(delta(previous == null || latest == null ? null : latest.avgMental() - previous.avgMental()))));

        long homeless = town.getAllResidents().stream().filter(r -> r.getHousePos() == null).count();
        long unemployed = town.getAllResidents().stream().filter(r -> r.getWorkPos() == null).count();
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

    private static MutableComponent label(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": "));
    }

    private static MutableComponent value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.AQUA);
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
