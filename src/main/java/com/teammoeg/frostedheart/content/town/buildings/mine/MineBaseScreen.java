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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MineBaseScreen extends StandardTownBuildingScreen<MineBaseMenu> {
    public MineBaseScreen(MineBaseMenu menu) {
        super(menu);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new WorkersTab(this));
        addTab(new ProductionTab(this));
        addTab(new MinesTab(this));
    }

    private abstract static class MineBaseTab extends AbstractTownTab<MineBaseMenu> {
        private MineBaseTab(MineBaseScreen screen) {
            super(screen);
        }

        @Override public CIcons.CIcon getIcon() { return INACTIVE_TAB; }
        @Override public CIcons.CIcon getActiveIcon() { return ACTIVE_TAB; }
    }

    private static final class OverviewTab extends MineBaseTab {
        private OverviewTab(MineBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(FHBlocks.MINE_BASE.get()); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine_base.overview");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> overviewRows(getMenu())));
        }
    }

    private static final class WorkersTab extends MineBaseTab {
        private WorkersTab(MineBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.IRON_PICKAXE); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine_base.workers");
        }
        @Override public void build(UILayer layer) {
            MineBaseBuilding building = getMenu().getBuilding().orElse(null);
            FHConfig.Server.Town.ResidentProgression progression =
                    FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
            layer.add(new TownWorkforcePanel(
                    layer, 8, 4,
                    getMenu()::getResidents,
                    resident -> building != null && building.canResidentWork(resident),
                    resident -> resident.getWorkProficiency(MineBaseBuilding.class),
                    resident -> building == null ? 0.0 : building.getResidentScore(resident),
                    resident -> building == null ? 0.0
                            : building.getResidentScore(resident)
                            * FHConfig.SERVER.TOWN.MINING.baseOutputPerStandardWorkerDay.get(),
                    resident -> ResidentAttributeModel.calculateDailyProficiencyGain(
                            resident.getWorkProficiency(MineBaseBuilding.class),
                            progression.proficiencyGrowthAtZeroPerWorkday.get(),
                            progression.minimumProficiencyGrowthPerWorkday.get()),
                    Component.translatable("gui.frostedheart.mine_base.mining_proficiency"),
                    value -> Component.translatable(
                            "gui.frostedheart.mine_base.personal_contribution", one(value))
            ));
        }
    }

    private static final class ProductionTab extends MineBaseTab {
        private ProductionTab(MineBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.RAW_IRON); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine_base.production");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> productionRows(getMenu())));
        }
    }

    private static final class MinesTab extends MineBaseTab {
        private MinesTab(MineBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(FHBlocks.MINE.get()); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine_base.mines");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> mineRows(getMenu())));
        }
    }

    private static List<TownInfoPanel.Row> overviewRows(MineBaseMenu menu) {
        MineBaseBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(title("gui.frostedheart.mine_base.overview"));
        rows.add(status("gui.frostedheart.town.workable", building.isBuildingWorkable()));
        if (!building.isBuildingWorkable()) {
            appendCommonFailures(rows, building);
        }
        rows.add(text("gui.frostedheart.town.workers_count",
                menu.getResidents().size(), building.getMaxResidents()));
        rows.add(text("gui.frostedheart.town.area", building.getArea()));
        rows.add(text("gui.frostedheart.town.volume", building.getVolume()));
        rows.add(text("gui.frostedheart.mine_base.linked_mines", building.getLinkedMines().size()));
        return rows;
    }

    private static List<TownInfoPanel.Row> productionRows(MineBaseMenu menu) {
        MineBaseBuilding building = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (building == null || town == null) return missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        MineBaseBuilding.MiningForecast forecast = building.getForecast(town);
        rows.add(title("gui.frostedheart.town.next_settlement_forecast"));
        rows.add(text("gui.frostedheart.mine_base.total_productivity", two(forecast.totalProductivity())));
        rows.add(text("gui.frostedheart.mine_base.planned_mining", one(forecast.requested())));
        rows.add(text("gui.frostedheart.mine_base.extractable", one(forecast.extractable())));
        if (forecast.stopReason() != TownProductionStopReason.NONE) {
            rows.add(reason(forecast.stopReason()));
        }
        rows.add(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.mine_base.forecast_ignores_storage"),
                0xFFAAAAAA));
        rows.add(TownInfoPanel.Row.empty());
        rows.add(title("gui.frostedheart.town.last_settlement"));
        MineBaseBuilding.MiningDailyReport report = building.getDailyReport();
        if (!report.hasData()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town.no_production_report"),
                    0xFFAAAAAA));
            return rows;
        }
        rows.add(text("gui.frostedheart.mine_base.planned_mining", one(report.requested())));
        rows.add(text("gui.frostedheart.mine_base.actual_mining", one(report.extracted())));
        rows.add(text("gui.frostedheart.town.actual_stored", one(report.stored())));
        rows.add(text("gui.frostedheart.town.storage_loss", one(report.lost())));
        if (report.stopReason() != TownProductionStopReason.NONE) {
            rows.add(reason(report.stopReason()));
        }
        for (TownProductionReportItem item : report.items()) {
            rows.add(TownInfoPanel.Row.item(item.item(),
                    Component.translatable("gui.frostedheart.town.item_stored_produced",
                            item.item().getDescription(),
                            one(item.stored()), one(item.produced()))));
        }
        return rows;
    }

    private static List<TownInfoPanel.Row> mineRows(MineBaseMenu menu) {
        MineBaseBuilding building = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (building == null || town == null) return missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(title("gui.frostedheart.mine_base.mines"));
        rows.add(text("gui.frostedheart.mine_base.connection_radius",
                building.getConnectionRadius()));
        List<BlockPos> positions = building.getLinkedMines().stream()
                .sorted(Comparator.comparingLong(BlockPos::asLong))
                .toList();
        if (positions.isEmpty()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine_base.no_mines_in_range"),
                    0xFFAAAAAA));
            return rows;
        }
        for (BlockPos pos : positions) {
            ITownBuilding value = town.getTownBuilding(pos).orElse(null);
            rows.add(TownInfoPanel.Row.item(FHBlocks.MINE.get().asItem(),
                    Component.translatable("gui.frostedheart.town.coordinates",
                            pos.getX(), pos.getY(), pos.getZ())));
            if (value instanceof MineBuilding mine) {
                Component biome = Component.translatable(
                        Util.makeDescriptionId("biome", mine.getBiomePath()));
                rows.add(text("gui.frostedheart.mine.biome", biome));
                rows.add(status("gui.frostedheart.mine.mine_usable", mine.isBuildingWorkable()));
            } else {
                rows.add(TownInfoPanel.Row.colored(
                        Component.translatable("gui.frostedheart.mine_base.missing_mine"),
                        0xFFFF5555));
            }
        }
        return rows;
    }

    public static void appendCommonFailures(
            List<TownInfoPanel.Row> rows, AbstractTownBuilding building
    ) {
        if (!building.isInitialized()) rows.add(failure("gui.frostedheart.town.failure.uninitialized"));
        if (building.isOccupiedAreaOverlapped()) rows.add(failure("gui.frostedheart.town.failure.overlap"));
        if (!building.isStructureValid()) rows.add(failure("gui.frostedheart.town.failure.structure"));
    }

    public static TownInfoPanel.Row reason(TownProductionStopReason reason) {
        return TownInfoPanel.Row.colored(
                Component.literal("• ").append(Component.translatable(
                        "gui.frostedheart.town.stop." + reason.name().toLowerCase(Locale.ROOT))),
                0xFFFF5555);
    }

    public static TownInfoPanel.Row title(String key) {
        return TownInfoPanel.Row.colored(Component.translatable(key), 0xFFFFAA00);
    }

    public static TownInfoPanel.Row text(String key, Object... args) {
        return TownInfoPanel.Row.text(Component.translatable(key, args));
    }

    public static TownInfoPanel.Row status(String key, boolean value) {
        return TownInfoPanel.Row.colored(
                Component.translatable(key, Component.translatable(value
                        ? "gui.frostedheart.common.yes"
                        : "gui.frostedheart.common.no")),
                value ? 0xFF55FF55 : 0xFFFF5555);
    }

    public static TownInfoPanel.Row failure(String key, Object... args) {
        return TownInfoPanel.Row.colored(
                Component.literal("• ").append(Component.translatable(key, args)),
                0xFFFF5555);
    }

    public static List<TownInfoPanel.Row> missingBuildingRows() {
        return List.of(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.town.missing_building"),
                0xFFFF5555));
    }

    public static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String two(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
