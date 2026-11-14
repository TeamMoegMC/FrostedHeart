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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
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
import com.teammoeg.frostedheart.content.town.buildings.hunting.view.HuntingBaseViewPresenter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 狩猎基地界面：布告板的概览、猎人、产出三页；详细模式下的调试页保留原来的文字面板。
 * <p>
 * The hunting base screen on the notice board: overview, hunters and output; the debug tab in detailed mode keeps
 * the original text panels.
 */
public class HuntingBaseScreen extends WorkBoardScreen<HuntingBaseMenu> {
    public HuntingBaseScreen(HuntingBaseMenu menu) {
        super(menu);
    }

    @Override
    protected WorkView.Board present() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        return HuntingBaseViewPresenter.present(new HuntingBaseViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(), menu.getResidents(),
                town, town == null ? "" : town.getName(), day));
    }

    @Override
    protected CIcons.CIcon buildingIcon() {
        return CIcons.getIcon(FHBlocks.HUNTING_BASE.get());
    }

    @Override
    protected CIcons.CIcon crewIcon() {
        return CIcons.getIcon(Items.BOW);
    }

    @Override
    protected CIcons.CIcon outputIcon() {
        return CIcons.getIcon(Items.BEEF);
    }

    @Override
    protected String key() {
        return HuntingBaseViewPresenter.KEY;
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
                out.append(resident).append(String.format(Locale.ROOT, ": score %.3f, proficiency %.3f, can work %s%n",
                        building.getResidentScore(resident), resident.getWorkProficiency(HuntingBaseBuilding.class), building.canResidentWork(resident)));
        });
        return out.toString();
    }

    private static UIElement workforcePanel(UILayer layer, int x, int y, HuntingBaseMenu menu) {
        HuntingBaseBuilding building = menu.getBuilding().orElse(null);
        FHConfig.Server.Town.ResidentProgression progression = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        return new TownWorkforcePanel(
                layer, x, y,
                menu::getResidents,
                resident -> building != null && building.canResidentWork(resident),
                resident -> resident.getWorkProficiency(HuntingBaseBuilding.class),
                resident -> building == null ? 0.0 : building.getResidentScore(resident),
                resident -> building == null ? 0.0
                        : building.getResidentScore(resident) * FHConfig.SERVER.TOWN.HUNTING.expectedLootRollsPerStandardWorkerDay.get(),
                resident -> ResidentAttributeModel.calculateDailyProficiencyGain(
                        resident.getWorkProficiency(HuntingBaseBuilding.class),
                        progression.proficiencyGrowthAtZeroPerWorkday.get(),
                        progression.minimumProficiencyGrowthPerWorkday.get(),
                        progression.maximumWorkProficiency.get()),
                Component.translatable("gui.frostedheart.hunting_base.hunting_proficiency"),
                value -> Component.translatable("gui.frostedheart.hunting_base.personal_contribution", MineBaseScreen.two(value)));
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
        FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        double spaceRating = TownMathFunctions.calculateSpaceRating(
                building.getVolume(),
                building.getArea(),
                scoring.spaceAreaCoefficient.get(),
                scoring.spaceHeightLogCoefficient.get(),
                scoring.spaceHeightLogOffset.get(),
                scoring.spaceResponseScale.get(),
                scoring.spaceResponseExponent.get());
        double temperatureRating = TownMathFunctions.calculateTemperatureRating(
                building.getEffectiveTemperature(),
                scoring.comfortableTemperatureCelsius.get(),
                scoring.minimumTemperatureRating.get(),
                scoring.temperatureRatingSlope.get(),
                scoring.temperatureRatingHalfPointDifferenceCelsius.get());
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
