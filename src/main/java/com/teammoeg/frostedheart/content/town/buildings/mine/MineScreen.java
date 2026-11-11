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
import java.util.Map;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.buildings.mine.view.MineViewPresenter;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.NoteStack;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardBuildingScreen;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.client.board.PaperCard;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Stat;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

/**
 * 矿井界面：布告板的概览页（矿藏、连着的采矿基地、地貌和能挖出的东西）；详细模式下的调试页保留原来的文字面板。
 * <p>
 * The mine screen on the notice board: an overview of the deposit, the mine base it feeds, the ground and what it
 * yields; the debug tab in detailed mode keeps the original text panels.
 */
public class MineScreen extends NoticeBoardBuildingScreen<MineMenu> {
    private MineViewPresenter.MineView view;

    public MineScreen(MineMenu menu) {
        super(menu);
    }

    private static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(MineViewPresenter.KEY + key, args);
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    @Override
    protected void addTabs(List<Tab> tabs, boolean detailed) {
        tabs.add(new Tab(CIcons.getIcon(FHBlocks.MINE.get()), t("tab.overview"), false, this::buildOverview));
        if (detailed) tabs.add(new Tab(CIcons.getIcon(Items.SPYGLASS), b("detailed"), true, this::buildDebug));
    }

    @Override
    protected void beforeRebuild() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        view = MineViewPresenter.present(new MineViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(), town,
                town == null ? "" : town.getName(), day));
        composeNotes(view.notes(), view.talk());
        trackStatus(view.header().status());
        trackFrost(view.header().frost());
    }

    private void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, view.header(), CIcons.getIcon(FHBlocks.MINE.get()),
                menu.getTown().map(TeamTown::getName).orElse(""), this::stampScale);
    }

    private void buildOverview(UILayer layer) {
        header(layer, true);
        renameTag(layer, view.header().name());
        int w = contentWidth(), h = contentHeight(), top = 38, gap = 6, statH = 46;
        List<Stat> stats = view.stats();
        if (stats.isEmpty()) {
            PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, b("reason.missing"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
            card.setPosAndSize(w / 6, top + 10, w * 2 / 3, 44);
            layer.add(card);
            return;
        }
        int[] widths = BoardWidgets.widths(w, gap, 1, 1.4, 1.2);
        CTextureIcon[] pins = {NoticeBoardIcons.PIN_RED, NoticeBoardIcons.PIN_BLUE, NoticeBoardIcons.PIN_GREEN};
        float[] tilts = {0.4f, -0.5f, 0.3f};
        int x = 0;
        for (int i = 0; i < stats.size(); i++) {
            BoardWidgets.stat(layer, stats.get(i), x, top, widths[i], statH, pins[i % pins.length], tilts[i % tilts.length], 0, this::openTab);
            x += widths[i] + gap;
        }
        int lowerTop = top + statH + 10;
        int leftW = (int) (w * 0.58) - gap;
        int conditionsH = BoardWidgets.conditions(layer, b("work.conditions"), view.conditions(), 0, lowerTop, leftW, 0);
        int sheetTop = lowerTop + conditionsH + 8;
        if (h - sheetTop >= 40) {
            int sheetH = BoardWidgets.items(layer, t("composition"), view.composition(), t("composition.none"), 2, sheetTop, leftW - 4,
                    h - sheetTop - 14, NoticeBoardIcons.PIN_BLUE, -0.3f, 0, PaperCard.Kind.GRID);
            BoardWidgets.woodText(layer, view.compositionNote(), 6, sheetTop + sheetH + 4, leftW - 12, List.of(), null);
        }
        int stackX = leftW + gap + 4, stackW = w - stackX - 6;
        NoteStack stack = noteStack(layer, stackX, lowerTop + 4, stackW, h - lowerTop - 10 - 70);
        int tipsTop = lowerTop + 4 + stack.getHeight() + 10;
        BoardWidgets.tips(layer, b("work.tips"), view.tips(), stackX + 2, tipsTop, stackW - 4, h - tipsTop - 2);
    }

    private void buildDebug(UILayer layer) {
        header(layer, false);
        List<BoardWidgets.DebugSection> sections = List.of(
                new BoardWidgets.DebugSection(b("work.debug.overview"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> overviewRows(menu))),
                new BoardWidgets.DebugSection(t("debug.deposit"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> depositRows(menu))));
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), sections, debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, () -> {
            StringBuilder out = new StringBuilder(view.header().name().getString()).append(" @ ").append(pos().toShortString()).append('\n');
            for (List<TownInfoPanel.Row> rows : List.of(overviewRows(menu), depositRows(menu))) {
                for (TownInfoPanel.Row row : rows) out.append(row.text().getString()).append('\n');
                out.append('\n');
            }
            return out.toString();
        });
    }

    private static List<TownInfoPanel.Row> overviewRows(MineMenu menu) {
        MineBuilding mine = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (mine == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.mine.overview"));
        rows.add(MineBaseScreen.status(
                "gui.frostedheart.mine.building_usable", mine.isBuildingWorkable()));
        if (!mine.isBuildingWorkable()) {
            MineBaseScreen.appendCommonFailures(rows, mine);
        }

        MineBaseBuilding connectedBase = town == null ? null
                : town.getTownBuildings().values().stream()
                .filter(MineBaseBuilding.class::isInstance)
                .map(MineBaseBuilding.class::cast)
                .filter(base -> base.getLinkedMines().contains(mine.getPos()))
                .sorted(Comparator.comparingLong(base -> base.getPos().asLong()))
                .findFirst()
                .orElse(null);
        if (connectedBase == null) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.not_connected"),
                    0xFFFF5555));
        } else {
            rows.add(MineBaseScreen.text(
                    "gui.frostedheart.mine.connected_base",
                    connectedBase.getPos().getX(),
                    connectedBase.getPos().getY(),
                    connectedBase.getPos().getZ()));
            if (!connectedBase.isBuildingWorkable()) {
                rows.add(TownInfoPanel.Row.colored(
                        Component.translatable("gui.frostedheart.mine.connected_base_unworkable"),
                        0xFFFF5555));
            }
        }
        Component biome = Component.translatable(
                Util.makeDescriptionId("biome", mine.getBiomePath()));
        rows.add(MineBaseScreen.text("gui.frostedheart.mine.biome", biome));
        return rows;
    }

    private static List<TownInfoPanel.Row> depositRows(MineMenu menu) {
        MineBuilding mine = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (mine == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        ChunkPos chunk = new ChunkPos(mine.getPos());
        double maximum = TerrainResourceType.ORE.getResourcePerSq();
        double remaining = town == null
                ? maximum
                : town.getRemainingTerrainResource(TerrainResourceType.ORE, chunk);
        double ratio = maximum <= 0.0 ? 0.0 : remaining / maximum;

        rows.add(MineBaseScreen.title("gui.frostedheart.mine.chunk_deposit"));
        rows.add(MineBaseScreen.text("gui.frostedheart.mine.chunk_coordinates", chunk.x, chunk.z));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.mine.remaining_deposit",
                Math.round(remaining), Math.round(maximum)));
        rows.add(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.mine.remaining_percent",
                        String.format(Locale.ROOT, "%.1f", ratio * 100.0)),
                remaining <= 0.0 ? 0xFFFF5555 : 0xFFFFFFFF));
        if (remaining <= 0.0) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.exhausted"),
                    0xFFFF5555));
        }
        rows.add(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.mine.shared_chunk_deposit"),
                0xFFAAAAAA));
        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.mine.output_composition"));

        Map<Item, Integer> weights = MineBuilding.getWeights(mine.getBiomePath());
        if (!MineBuilding.hasBiomeRecipe(mine.getBiomePath())) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.default_composition"),
                    0xFFAAAAAA));
        }
        double totalWeight = weights.values().stream()
                .filter(weight -> weight > 0)
                .mapToDouble(Integer::doubleValue)
                .sum();
        weights.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getDescriptionId()))
                .forEach(entry -> {
                    double percentage = totalWeight <= 0.0
                            ? 0.0
                            : entry.getValue() * 100.0 / totalWeight;
                    rows.add(TownInfoPanel.Row.item(
                            entry.getKey(),
                            Component.translatable(
                                    "gui.frostedheart.mine.output_weight",
                                    entry.getKey().getDescription(),
                                    String.format(Locale.ROOT, "%.1f", percentage))));
                });
        return rows;
    }
}
