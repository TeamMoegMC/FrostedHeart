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

package com.teammoeg.frostedheart.content.town.client.board;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Duty;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Person;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Stat;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Forecast;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Output;
import com.teammoeg.frostedheart.content.town.ui.view.WorkView.Site;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 有工人的生产建筑的布告板：概览、工人、产出三页，有矿井的再加一页矿井，详细模式下多一页调试。
 * 各建筑只提供视图（{@link WorkView.Board}）、图标和调试面板。
 * <p>
 * The notice board of the staffed production buildings: overview, crew and output, a sites tab for buildings with
 * linked sites, and a debug tab in detailed mode. Each building supplies only its view ({@link WorkView.Board}), its
 * icons and its debug panels.
 */
public abstract class WorkBoardScreen<C extends CBlockEntityMenu<? extends AbstractTownBuildingBlockEntity>> extends NoticeBoardBuildingScreen<C> {
    protected static final int OVERVIEW = 0, CREW = 1, OUTPUT = 2, SITES = 3;
    private static final CTextureIcon[] STAT_PINS = {NoticeBoardIcons.PIN_RED, NoticeBoardIcons.PIN_BLUE, NoticeBoardIcons.PIN_GREEN,
            NoticeBoardIcons.PIN_BRASS};
    private static final float[] STAT_TILTS = {0.5f, -0.4f, 0.3f, -0.6f};

    protected WorkView.Board board;

    protected WorkBoardScreen(C menu) {
        super(menu);
    }

    /** The board's contents, computed before every rebuild. */
    protected abstract WorkView.Board present();

    protected abstract CIcons.CIcon buildingIcon();

    protected abstract CIcons.CIcon crewIcon();

    protected abstract CIcons.CIcon outputIcon();

    /** The prefix of the building's own text keys, such as {@code gui.frostedheart.board.mine_base.}. */
    protected abstract String key();

    protected abstract String townName();

    protected abstract List<BoardWidgets.DebugSection> debugSections();

    protected abstract String debugText();

    /** The icon of the sites tab, or null when the building has no linked sites. */
    @Nullable
    protected CIcons.CIcon sitesIcon() {
        return null;
    }

    protected Component t(String k, Object... args) {
        return Component.translatable(key() + k, args);
    }

    protected static Component b(String k, Object... args) {
        return Component.translatable(TownFormat.KEY + k, args);
    }

    @Override
    protected void addTabs(List<Tab> tabs, boolean detailed) {
        tabs.add(new Tab(buildingIcon(), t("tab.overview"), false, this::buildOverview));
        tabs.add(new Tab(crewIcon(), t("tab.crew"), false, this::buildCrew));
        tabs.add(new Tab(outputIcon(), t("tab.output"), false, this::buildOutput));
        if (sitesIcon() != null) tabs.add(new Tab(sitesIcon(), t("tab.sites"), false, this::buildSites));
        if (detailed) tabs.add(new Tab(CIcons.getIcon(Items.SPYGLASS), b("detailed"), true, this::buildDebug));
    }

    @Override
    protected void beforeRebuild() {
        board = present();
        composeNotes(board.notes(), board.talk());
        trackStatus(board.header().status());
        trackFrost(board.header().frost());
    }

    protected void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, board.header(), buildingIcon(), townName(), this::stampScale);
    }

    protected int sheetFrost() {
        return BoardWidgets.sheetFrost(board.header().frost());
    }

    // ------------------------------------------------------------------ overview

    protected void buildOverview(UILayer layer) {
        header(layer, true);
        renameTag(layer, board.header().name());
        int w = contentWidth(), h = contentHeight();
        int top = 38, gap = 6, statH = 46, frost = sheetFrost();
        List<Stat> stats = board.stats();
        if (stats.isEmpty()) {
            PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, b("reason.missing"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
            card.setPosAndSize(w / 6, top + 10, w * 2 / 3, 44);
            layer.add(card);
            return;
        }
        int[] widths = BoardWidgets.widths(w, gap, 1, 1, 1, 1);
        int x = 0;
        for (int i = 0; i < stats.size(); i++) {
            BoardWidgets.stat(layer, stats.get(i), x, top, widths[i], statH, STAT_PINS[i % STAT_PINS.length], STAT_TILTS[i % STAT_TILTS.length],
                    frost, this::openTab);
            x += widths[i] + gap;
        }
        int lowerTop = top + statH + 10;
        int leftW = (int) (w * 0.58) - gap;
        int conditionsH = BoardWidgets.conditions(layer, b("work.conditions"), board.conditions(), 0, lowerTop, leftW, frost);
        int stripTop = lowerTop + conditionsH + 10;
        List<Person> onDuty = board.crew().people().stream().filter(p -> p.duty() == Duty.ON_DUTY).toList();
        boolean big = h - stripTop >= 14 + 58 + 4;
        if (h - stripTop >= 60)
            BoardWidgets.photoStrip(layer, b("work.on_duty"), onDuty, 2, stripTop, leftW, big ? 48 : 36, big ? 58 : 44, person -> {
                selectedPerson = person.id();
                openTab(CREW);
            });
        int stackX = leftW + gap + 4, stackW = w - stackX - 6;
        NoteStack stack = noteStack(layer, stackX, lowerTop + 4, stackW, h - lowerTop - 10 - BoardWidgets.SLIP_H - 10);
        int slipTop = lowerTop + 4 + stack.getHeight() + 10;
        Output output = board.output();
        if (h - slipTop >= BoardWidgets.SLIP_H)
            BoardWidgets.itemSlip(layer, output.yieldTitle(), output.yield(), output.yieldEmpty(), stackX + 4, slipTop, stackW - 8,
                    () -> openTab(OUTPUT));
    }

    // ------------------------------------------------------------------ crew

    protected void buildCrew(UILayer layer) {
        header(layer, false);
        WorkView.Crew crew = board.crew();
        BoardWidgets.workers(layer, 0, 30, contentWidth(), contentHeight() - 30, crew.headline(), crew.people(), crew.slots(), crew.emptySlots(),
                selectedPerson, id -> {
                    selectedPerson = id;
                    refreshElements();
                }, crew.empty(), List.of(b("person.efficiency.tooltip")));
    }

    // ------------------------------------------------------------------ output

    protected void buildOutput(UILayer layer) {
        header(layer, false);
        Output output = board.output();
        int w = contentWidth(), h = contentHeight();
        int top = 34, frost = sheetFrost();
        int leftW = (int) (w * 0.56);
        int sheetH = BoardWidgets.items(layer, output.yieldTitle(), output.yield(), output.yieldEmpty(), 2, top, leftW - 8,
                h - top - 18, NoticeBoardIcons.PIN_BLUE, -0.4f, frost, PaperCard.Kind.GRID);
        if (!output.yieldNote().getString().isEmpty())
            layer.add(new HitArea(layer, 6, top + sheetH + 6, leftW - 12, 10).painter((g, px, py, pw, ph) ->
                    BoardDraw.textOnWood(g, output.yieldNote(), px, py, pw, output.yieldTone() == com.teammoeg.frostedheart.content.town.ui.view.BoardView.Tone.WARN
                            ? 0xFFFAC775 : NoticeBoardTheme.ON_WOOD)));

        int rightX = leftW + 6, rightW = w - rightX - 4, y = top, cardH = 46;
        float tilt = 0.4f;
        for (Forecast forecast : output.forecast()) {
            if (y + cardH > h) break;
            BoardWidgets.stat(layer, new Stat(forecast.title(), forecast.value(), forecast.line(), forecast.tone(), forecast.tooltip()),
                    rightX, y, rightW, cardH, NoticeBoardIcons.PIN_RED, tilt, frost, this::openTab);
            y += cardH + 8;
            tilt = -tilt;
        }
        BoardWidgets.tips(layer, b("work.tips"), output.tips(), rightX + 2, y + 2, rightW - 4, h - y - 4);
    }

    // ------------------------------------------------------------------ sites

    protected void buildSites(UILayer layer) {
        header(layer, false);
        int w = contentWidth(), h = contentHeight(), top = 30;
        BoardWidgets.woodText(layer, board.sitesTitle(), 2, top, w - 4, List.of(), null);
        List<Site> sites = board.sites();
        if (sites.isEmpty()) {
            PaperCard empty = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, board.sitesEmpty(), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 4));
            empty.setPosAndSize(w / 8, top + 20, w * 3 / 4, 56);
            layer.add(empty);
            return;
        }
        int columns = w >= 420 ? 2 : 1, gap = 8, cardW = (w - gap * (columns - 1) - 4) / columns, cardH = 44;
        ScrollColumn column = new ScrollColumn(layer, 8, scroll, c -> {
            for (int i = 0; i < sites.size(); i += columns) {
                List<Site> row = sites.subList(i, Math.min(sites.size(), i + columns));
                UILayer line = new UILayer(c) {
                    @Override
                    public void addUIElements() {
                        int x = 2;
                        float tilt = -0.3f;
                        for (Site site : row) {
                            add(siteCard(this, site, x, cardW, cardH, tilt));
                            x += cardW + gap;
                            tilt = -tilt;
                        }
                    }

                    @Override
                    public void alignWidgets() {
                    }
                };
                line.setScissorEnabled(false);
                line.setSize(w, cardH + 2);
                c.add(line);
            }
        }, value -> scroll = value);
        column.setPosAndSize(0, top + 16, w, h - top - 16);
        layer.add(column);
    }

    private PaperCard siteCard(UILayer parent, Site site, int x, int w, int h, float tilt) {
        PaperCard card = new PaperCard(parent).pin(NoticeBoardIcons.PIN_BLUE).tilt(tilt).painter((g, cx, cy, cw, ch) -> {
            g.renderItem(site.icon(), cx + 7, cy + 7);
            int textX = cx + 28, textW = cw - 36;
            int stateW = BoardDraw.chipWidth(site.state());
            BoardDraw.text(g, site.name().copy().withStyle(ChatFormatting.BOLD), textX, cy + 7, textW - stateW - 6, NoticeBoardTheme.INK);
            BoardDraw.chip(g, site.state(), cx + cw - 8 - stateW, cy + 5, BoardDraw.toneColor(site.tone()));
            BoardDraw.text(g, site.detail(), textX, cy + 19, textW, NoticeBoardTheme.MUTED);
            if (site.percent() >= 0) {
                Component percent = b("work.site.left", site.percent());
                int percentW = BoardDraw.font().width(percent);
                NoticeBoardTheme.INSTANCE.drawProgressBar(g, textX, cy + 32, textW - percentW - 6, 5, site.percent() / 100f,
                        BoardDraw.toneFill(site.tone()));
                BoardDraw.textRight(g, percent, cx + cw - 8, cy + 30, NoticeBoardTheme.INK);
            }
        });
        card.setPosAndSize(x, 1, w, h);
        return card;
    }

    // ------------------------------------------------------------------ debug

    protected void buildDebug(UILayer layer) {
        header(layer, false);
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), debugSections(), debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, this::debugText);
    }
}
