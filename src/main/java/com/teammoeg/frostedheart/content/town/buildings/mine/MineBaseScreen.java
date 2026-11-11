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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.building.TownProductionReportItem;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
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
import com.teammoeg.frostedheart.content.town.buildings.mine.view.MineBaseViewPresenter;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 采矿基地界面：布告板的概览、矿工、产出、矿井四页；详细模式下的调试页保留原来的文字面板。
 * <p>
 * The mine base screen on the notice board: overview, miners, output and mines; the debug tab in detailed mode keeps
 * the original text panels.
 */
public class MineBaseScreen extends WorkBoardScreen<MineBaseMenu> {
    public MineBaseScreen(MineBaseMenu menu) {
        super(menu);
    }

    @Override
    protected WorkView.Board present() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        return MineBaseViewPresenter.present(new MineBaseViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(), menu.getResidents(),
                town, town == null ? "" : town.getName(), day));
    }

    @Override
    protected CIcons.CIcon buildingIcon() {
        return CIcons.getIcon(FHBlocks.MINE_BASE.get());
    }

    @Override
    protected CIcons.CIcon crewIcon() {
        return CIcons.getIcon(Items.IRON_PICKAXE);
    }

    @Override
    protected CIcons.CIcon outputIcon() {
        return CIcons.getIcon(Items.RAW_IRON);
    }

    @Override
    protected CIcons.CIcon sitesIcon() {
        return CIcons.getIcon(FHBlocks.MINE.get());
    }

    @Override
    protected String key() {
        return MineBaseViewPresenter.KEY;
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
                new BoardWidgets.DebugSection(b("work.debug.output"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> productionRows(menu))),
                new BoardWidgets.DebugSection(b("work.debug.sites"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> mineRows(menu))));
    }

    @Override
    protected String debugText() {
        StringBuilder out = new StringBuilder(board.header().name().getString()).append(" @ ").append(pos().toShortString()).append('\n');
        for (List<TownInfoPanel.Row> rows : List.of(overviewRows(menu), productionRows(menu), mineRows(menu))) {
            for (TownInfoPanel.Row row : rows) out.append(row.text().getString()).append('\n');
            out.append('\n');
        }
        menu.getBuilding().ifPresent(building -> {
            for (Resident resident : menu.getResidents())
                out.append(resident).append(String.format(Locale.ROOT, ": score %.3f, proficiency %.3f, can work %s%n",
                        building.getResidentScore(resident), resident.getWorkProficiency(MineBaseBuilding.class), building.canResidentWork(resident)));
        });
        return out.toString();
    }

    private static UIElement workforcePanel(UILayer layer, int x, int y, MineBaseMenu menu) {
        MineBaseBuilding building = menu.getBuilding().orElse(null);
        FHConfig.Server.Town.ResidentProgression progression = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        return new TownWorkforcePanel(
                layer, x, y,
                menu::getResidents,
                resident -> building != null && building.canResidentWork(resident),
                resident -> resident.getWorkProficiency(MineBaseBuilding.class),
                resident -> building == null ? 0.0 : building.getResidentScore(resident),
                resident -> building == null ? 0.0
                        : building.getResidentScore(resident) * FHConfig.SERVER.TOWN.MINING.baseOutputPerStandardWorkerDay.get(),
                resident -> ResidentAttributeModel.calculateDailyProficiencyGain(
                        resident.getWorkProficiency(MineBaseBuilding.class),
                        progression.proficiencyGrowthAtZeroPerWorkday.get(),
                        progression.minimumProficiencyGrowthPerWorkday.get(),
                        progression.maximumWorkProficiency.get()),
                Component.translatable("gui.frostedheart.mine_base.mining_proficiency"),
                value -> Component.translatable("gui.frostedheart.mine_base.personal_contribution", one(value)));
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
