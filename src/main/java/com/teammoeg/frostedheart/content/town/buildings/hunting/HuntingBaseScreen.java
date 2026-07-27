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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HuntingBaseScreen extends StandardTownBuildingScreen<HuntingBaseMenu> {
    public HuntingBaseScreen(HuntingBaseMenu menu) {
        super(menu);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new WorkersTab(this));
        addTab(new ProductionTab(this));
    }

    private abstract static class HuntingTab extends AbstractTownTab<HuntingBaseMenu> {
        private HuntingTab(HuntingBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getIcon() { return INACTIVE_TAB; }
        @Override public CIcons.CIcon getActiveIcon() { return ACTIVE_TAB; }
    }

    private static final class OverviewTab extends HuntingTab {
        private OverviewTab(HuntingBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(FHBlocks.HUNTING_BASE.get());
        }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.hunting_base.overview");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> overviewRows(getMenu())));
        }
    }

    private static final class WorkersTab extends HuntingTab {
        private WorkersTab(HuntingBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.BOW); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.hunting_base.workers");
        }
        @Override public void build(UILayer layer) {
            HuntingBaseBuilding building = getMenu().getBuilding().orElse(null);
            FHConfig.Server.Town.ResidentProgression progression =
                    FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
            layer.add(new TownWorkforcePanel(
                    layer, 8, 4,
                    getMenu()::getResidents,
                    resident -> building != null && building.canResidentWork(resident),
                    resident -> resident.getWorkProficiency(HuntingBaseBuilding.class),
                    resident -> building == null ? 0.0 : building.getResidentScore(resident),
                    resident -> building == null ? 0.0
                            : building.getResidentScore(resident)
                            * FHConfig.SERVER.TOWN.HUNTING.expectedLootRollsPerStandardWorkerDay.get(),
                    resident -> ResidentAttributeModel.calculateDailyProficiencyGain(
                            resident.getWorkProficiency(HuntingBaseBuilding.class),
                            progression.proficiencyGrowthAtZeroPerWorkday.get(),
                            progression.minimumProficiencyGrowthPerWorkday.get()),
                    Component.translatable("gui.frostedheart.hunting_base.hunting_proficiency"),
                    value -> Component.translatable(
                            "gui.frostedheart.hunting_base.personal_contribution",
                            MineBaseScreen.two(value))
            ));
        }
    }

    private static final class ProductionTab extends HuntingTab {
        private ProductionTab(HuntingBaseScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.BEEF); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.hunting_base.production");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> productionRows(getMenu())));
        }
    }

    private static List<TownInfoPanel.Row> overviewRows(HuntingBaseMenu menu) {
        HuntingBaseBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return MineBaseScreen.missingBuildingRows();
        FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.hunting_base.overview"));
        rows.add(MineBaseScreen.status(
                "gui.frostedheart.town.workable", building.isBuildingWorkable()));
        if (!building.isBuildingWorkable()) {
            MineBaseScreen.appendCommonFailures(rows, building);
            if (building.getArea() < config.minimumFloorAreaBlocks.get()) {
                rows.add(MineBaseScreen.failure(
                        "gui.frostedheart.town.failure.area",
                        building.getArea(), config.minimumFloorAreaBlocks.get()));
            }
            if (building.getVolume() < config.minimumInteriorVolumeBlocks.get()) {
                rows.add(MineBaseScreen.failure(
                        "gui.frostedheart.town.failure.volume",
                        building.getVolume(), config.minimumInteriorVolumeBlocks.get()));
            }
            if (!building.isTemperatureValid()) {
                rows.add(MineBaseScreen.failure(
                        "gui.frostedheart.town.failure.temperature_low",
                        MineBaseScreen.one(building.getEffectiveTemperature()),
                        MineBaseScreen.one(config.minimumWorkingTemperatureCelsius.get())));
            }
        }
        rows.add(MineBaseScreen.text("gui.frostedheart.town.workers_count",
                menu.getResidents().size(), building.getMaxResidents()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.area", building.getArea()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.volume", building.getVolume()));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.effective_temperature",
                MineBaseScreen.one(building.getEffectiveTemperature()),
                Component.translatable(building.isTemperatureValid()
                        ? "gui.frostedheart.hunting_base.temperature_workable"
                        : "gui.frostedheart.hunting_base.temperature_low")));
        if (building.getTemperatureModifier() > 0.0) {
            rows.add(MineBaseScreen.text(
                    "gui.frostedheart.hunting_base.heating_active",
                    MineBaseScreen.one(building.getTemperatureModifier())));
        } else {
            rows.add(MineBaseScreen.text("gui.frostedheart.hunting_base.heating_inactive"));
        }
        double spaceRating = TownMathFunctions.calculateSpaceRating(
                building.getVolume(), building.getArea());
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(
                building.getEffectiveTemperature());
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.space_quality", percent(spaceRating)));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.temperature_quality", percent(temperatureRating)));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.overall_quality", percent(building.getRating())));
        return rows;
    }

    private static List<TownInfoPanel.Row> productionRows(HuntingBaseMenu menu) {
        HuntingBaseBuilding building = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (building == null || town == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        HuntingBaseBuilding.HuntingForecast forecast = building.getForecast(town);
        rows.add(MineBaseScreen.title("gui.frostedheart.town.next_settlement_forecast"));
        rows.add(MineBaseScreen.text("gui.frostedheart.hunting_base.total_productivity",
                MineBaseScreen.two(forecast.totalProductivity())));
        rows.add(MineBaseScreen.text("gui.frostedheart.hunting_base.new_expected_rolls",
                MineBaseScreen.two(forecast.newExpectedRolls())));
        rows.add(MineBaseScreen.text("gui.frostedheart.hunting_base.planned_rolls",
                forecast.plannedRolls()));
        rows.add(MineBaseScreen.text("gui.frostedheart.hunting_base.available_rolls",
                forecast.availableRolls()));
        if (forecast.stopReason() != TownProductionStopReason.NONE) {
            rows.add(MineBaseScreen.reason(forecast.stopReason()));
        }
        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.town.last_settlement"));
        HuntingBaseBuilding.HuntingDailyReport report = building.getDailyReport();
        if (!report.hasData()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town.no_production_report"),
                    0xFFAAAAAA));
            return rows;
        }
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.planned_rolls", report.plannedRolls()));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.executed_rolls", report.executedRolls()));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.hunting_base.generated_loot", whole(report.generated())));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.town.actual_stored", whole(report.stored())));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.town.storage_loss", whole(report.lost())));
        if (report.stopReason() != TownProductionStopReason.NONE) {
            rows.add(MineBaseScreen.reason(report.stopReason()));
        }
        for (TownProductionReportItem item : report.items()) {
            rows.add(TownInfoPanel.Row.item(item.item(),
                    Component.translatable("gui.frostedheart.town.item_stored_produced",
                            item.item().getDescription(),
                            whole(item.stored()), whole(item.produced()))));
        }
        return rows;
    }

    private static String percent(double value) {
        double safe = Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
        return String.format(Locale.ROOT, "%.1f%%", safe * 100.0);
    }

    private static long whole(double value) {
        return Math.round(value);
    }
}
