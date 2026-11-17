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

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.WorkBoardScreen;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView;
import com.teammoeg.frostedheart.content.town.buildings.logistics.view.TransportStationViewPresenter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Transport-station overview, workforce, and daily production screen.
 */
/**
 * 货运站界面：布告板的概览、搬运工、运力三页；详细模式下的调试页保留原来的文字面板。
 * <p>
 * The transport station screen on the notice board: overview, carriers and capacity; the debug tab in detailed mode
 * keeps the original text panels.
 */
public class TransportStationScreen extends WorkBoardScreen<TransportStationMenu> {
    public TransportStationScreen(TransportStationMenu menu) {
        super(menu);
    }

    @Override
    protected WorkView.Board present() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        return TransportStationViewPresenter.present(new TransportStationViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(),
                menu.getResidents(), town, town == null ? "" : town.getName(), day));
    }

    @Override
    protected CIcons.CIcon buildingIcon() {
        return CIcons.getIcon(FHBlocks.TRANSPORT_STATION.get());
    }

    @Override
    protected CIcons.CIcon crewIcon() {
        return CIcons.getIcon(Items.CHEST);
    }

    @Override
    protected CIcons.CIcon outputIcon() {
        return CIcons.getIcon(Items.CHEST_MINECART);
    }

    @Override
    protected String key() {
        return TransportStationViewPresenter.KEY;
    }

    @Override
    protected String townName() {
        return menu.getTown().map(TeamTown::getName).orElse("");
    }

    @Override
    protected List<BoardWidgets.DebugSection> debugSections() {
        return List.of(
                new BoardWidgets.DebugSection(b("work.debug.overview"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> overviewRows(menu))),
                new BoardWidgets.DebugSection(b("work.debug.crew"), (l, x, y, w, h) -> workforcePanel(l, x, y, menu)),
                new BoardWidgets.DebugSection(b("work.debug.output"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> productionRows(menu))));
    }

    @Override
    protected String debugText() {
        StringBuilder out = new StringBuilder(board.header().name().getString()).append(" @ ").append(pos().toShortString()).append('\n');
        for (List<TownInfoPanel.Row> rows : List.of(overviewRows(menu), productionRows(menu))) {
            for (TownInfoPanel.Row row : rows) out.append(row.text().getString()).append('\n');
            out.append('\n');
        }
        menu.getBuilding().ifPresent(building -> {
            for (Resident resident : menu.getResidents())
                out.append(resident).append(String.format(Locale.ROOT, ": score %.3f, capacity %.3f, can work %s%n",
                        building.getResidentScore(resident), building.getResidentCapacityContribution(resident), building.canResidentWork(resident)));
        });
        return out.toString();
    }

    private static UIElement workforcePanel(UILayer layer, int x, int y, TransportStationMenu menu) {
        return new TownWorkforcePanel(
                layer, x, y,
                menu::getResidents,
                resident -> menu.getBuilding().map(building -> building.canResidentWork(resident)).orElse(false),
                TransportStationScreen::transportProficiency,
                resident -> menu.getBuilding().map(building -> building.getResidentScore(resident)).orElse(0.0),
                resident -> menu.getBuilding().map(building -> building.getResidentCapacityContribution(resident)).orElse(0.0),
                TransportStationScreen::dailyProficiencyGain,
                Component.translatable("gui.frostedheart.transport_station.transport_proficiency"),
                value -> Component.translatable("gui.frostedheart.transport_station.personal_contribution", MineBaseScreen.two(value)));
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
