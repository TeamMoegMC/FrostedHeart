/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport-station overview, workforce, and daily production screen.
 */
public class TransportStationScreen extends StandardTownBuildingScreen<TransportStationMenu> {
    public TransportStationScreen(TransportStationMenu menu) {
        super(menu);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new WorkersTab(this));
        addTab(new ProductionTab(this));
    }

    private abstract static class TransportStationTab extends AbstractTownTab<TransportStationMenu> {
        private TransportStationTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getIcon() {
            return INACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getActiveIcon() {
            return ACTIVE_TAB;
        }
    }

    private static final class OverviewTab extends TransportStationTab {
        private OverviewTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(FHBlocks.TRANSPORT_STATION.get());
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.transport_station.overview");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> overviewRows(getMenu())));
        }
    }

    private static final class WorkersTab extends TransportStationTab {
        private WorkersTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(Items.CHEST);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.transport_station.workers");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new TownWorkforcePanel(
                    layer, 8, 4,
                    getMenu()::getResidents,
                    resident -> getMenu().getBuilding()
                            .map(building -> building.canResidentWork(resident))
                            .orElse(false),
                    TransportStationScreen::transportProficiency,
                    resident -> getMenu().getBuilding()
                            .map(building -> building.getResidentScore(resident))
                            .orElse(0.0),
                    resident -> getMenu().getBuilding()
                            .map(building -> building.getResidentCapacityContribution(resident))
                            .orElse(0.0),
                    TransportStationScreen::dailyProficiencyGain,
                    Component.translatable("gui.frostedheart.transport_station.transport_proficiency"),
                    value -> Component.translatable(
                            "gui.frostedheart.transport_station.personal_contribution",
                            MineBaseScreen.two(value))
            ));
        }
    }

    private static final class ProductionTab extends TransportStationTab {
        private ProductionTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(Items.CHEST_MINECART);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.transport_station.production");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> productionRows(getMenu())));
        }
    }

    private static List<TownInfoPanel.Row> overviewRows(TransportStationMenu menu) {
        TransportStationBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return MineBaseScreen.missingBuildingRows();

        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.transport_station.overview"));
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
        }
        rows.add(MineBaseScreen.text("gui.frostedheart.town.workers_count",
                menu.getResidents().size(), building.getMaxResidents()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.area", building.getArea()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.volume", building.getVolume()));
        return rows;
    }

    private static List<TownInfoPanel.Row> productionRows(TransportStationMenu menu) {
        TransportStationBuilding building = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (building == null || town == null) return MineBaseScreen.missingBuildingRows();

        List<TownInfoPanel.Row> rows = new ArrayList<>();
        TransportStationBuilding.TransportStationForecast forecast = building.getForecast(town);
        rows.add(MineBaseScreen.title("gui.frostedheart.town.next_settlement_forecast"));
        rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.eligible_workers",
                forecast.workerCount()));
        rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.total_productivity",
                MineBaseScreen.two(forecast.totalProductivity())));
        rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.planned_capacity",
                MineBaseScreen.one(forecast.plannedCapacity())));
        if (forecast.stopReason() != TownProductionStopReason.NONE) {
            rows.add(MineBaseScreen.reason(forecast.stopReason()));
        }

        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.town.last_settlement"));
        TransportStationBuilding.TransportStationDailyReport report = building.getDailyReport();
        if (!report.hasData()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town.no_production_report"),
                    0xFFAAAAAA));
        } else {
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.eligible_workers",
                    report.workerCount()));
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.total_productivity",
                    MineBaseScreen.two(report.totalProductivity())));
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.planned_capacity",
                    MineBaseScreen.one(report.plannedCapacity())));
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.actual_capacity",
                    MineBaseScreen.one(report.addedCapacity())));
            if (report.stopReason() != TownProductionStopReason.NONE) {
                rows.add(MineBaseScreen.reason(report.stopReason()));
            }
        }

        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.transport_station.town_transport"));
        var townReport = town.getTransportState().getDailyReport();
        if (!townReport.hasData()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town.no_production_report"),
                    0xFFAAAAAA));
        } else {
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.town_total_capacity",
                    MineBaseScreen.one(townReport.totalCapacity())));
            rows.add(MineBaseScreen.text("gui.frostedheart.transport_station.town_reserved_capacity",
                    MineBaseScreen.one(townReport.reservedCapacity())));
        }
        return rows;
    }

    private static double transportProficiency(Resident resident) {
        return resident.getWorkProficiency(TransportStationBuilding.class);
    }

    private static double dailyProficiencyGain(Resident resident) {
        FHConfig.Server.Town.ResidentProgression progression =
                FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        return ResidentAttributeModel.calculateDailyProficiencyGain(
                resident.getWorkProficiency(TransportStationBuilding.class),
                progression.proficiencyGrowthAtZeroPerWorkday.get(),
                progression.minimumProficiencyGrowthPerWorkday.get(),
                progression.maximumWorkProficiency.get());
    }
}
