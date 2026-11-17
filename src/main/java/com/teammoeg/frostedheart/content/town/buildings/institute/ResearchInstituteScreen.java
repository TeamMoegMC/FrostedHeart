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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.building.TownProductionStopReason;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteView;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteView.*;
import com.teammoeg.frostedheart.content.town.buildings.institute.view.InstituteViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.HitArea;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardBuildingScreen;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.client.board.PaperCard;
import com.teammoeg.frostedheart.content.town.client.board.PhotoCard;
import com.teammoeg.frostedheart.content.town.client.board.ScrollColumn;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentAttributeModel;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeClientState;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

/**
 * 研究所界面：布告板主题的概览、研究员、协作研究、联想四页；详细模式下多一页调试，内容是原来的全部文字行。
 * 显示内容由 {@link InstituteViewPresenter} 算出，摆件来自 {@link BoardWidgets}。
 * <p>
 * The research institute screen in the notice-board theme: overview, researchers, collaborative research and
 * association, plus a debug tab in detailed mode holding the original text rows. {@link InstituteViewPresenter}
 * decides what is shown; {@link BoardWidgets} supplies the pieces.
 */
public class ResearchInstituteScreen extends NoticeBoardBuildingScreen<ResearchInstituteMenu> {
    private static final int OVERVIEW = 0, RESEARCHERS = 1, RESEARCH = 2, ASSOCIATION = 3;
    private static KnowledgeJournalData knowledge;
    private static long knowledgeSerial = -1;

    private InstituteView view;

    /** The past idea whose journal excerpt is pinned on the association page, or null. */
    @javax.annotation.Nullable
    private com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey selectedIdea;

    public ResearchInstituteScreen(ResearchInstituteMenu menu) {
        super(menu);
    }

    private static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(InstituteViewPresenter.KEY + key, args);
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    @Override
    protected long dataSerial() {
        return KnowledgeClientState.serial();
    }

    @Override
    protected void addTabs(List<Tab> tabs, boolean detailed) {
        tabs.add(new Tab(CIcons.getIcon(FHBlocks.RESEARCH_INSTITUTE.get()), t("tab.overview"), false, this::buildOverview));
        tabs.add(new Tab(CIcons.getIcon(Items.WRITABLE_BOOK), t("tab.researchers"), false, this::buildResearchers));
        tabs.add(new Tab(CIcons.getIcon(Items.BOOKSHELF), t("tab.research"), false, this::buildResearch));
        tabs.add(new Tab(CIcons.getIcon(Items.AMETHYST_SHARD), t("tab.association"), false, this::buildAssociation));
        if (detailed) tabs.add(new Tab(CIcons.getIcon(Items.SPYGLASS), b("detailed"), true, this::buildDebug));
    }

    @Override
    protected void beforeRebuild() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        view = InstituteViewPresenter.present(new InstituteViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(),
                menu.getResidents(), town, knowledge(), town == null ? "" : town.getName(), day));
        composeNotes(view.notes(), view.talk());
        trackStatus(view.header().status());
        trackFrost(view.header().frost());
    }

    private String townName() {
        return menu.getTown().map(TeamTown::getName).orElse("");
    }

    private void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, view.header(), CIcons.getIcon(FHBlocks.RESEARCH_INSTITUTE.get()), townName(), this::stampScale);
    }

    private int sheetFrost() {
        return BoardWidgets.sheetFrost(view.header().frost());
    }

    // ------------------------------------------------------------------ overview

    private void buildOverview(UILayer layer) {
        header(layer, true);
        renameTag(layer, view.header().name());
        Overview overview = view.overview();
        int w = contentWidth(), h = contentHeight();
        int top = 38, gap = 6, statH = 46, frost = sheetFrost();
        int[] widths = BoardWidgets.widths(w, gap, 76, 76, 128, 76);
        int x = 0;
        BoardWidgets.stat(layer, overview.researchers(), x, top, widths[0], statH, NoticeBoardIcons.PIN_RED, 0.5f, frost, this::openTab);
        x += widths[0] + gap;
        BoardWidgets.stat(layer, overview.hands(), x, top, widths[1], statH, NoticeBoardIcons.PIN_BLUE, -0.4f, frost, this::openTab);
        x += widths[1] + gap;
        Capacity capacity = overview.capacity();
        BoardWidgets.usage(layer, t("stat.capacity").copy().append(b("separator")).append(t("stat.capacity.shelves", capacity.shelves())),
                t("stat.capacity.archive"), capacity.archiveUsed(), capacity.archiveLimit(),
                t("stat.capacity.inbox"), capacity.inboxUsed(), capacity.inboxLimit(), capacity.tooltip(), x, top, widths[2], statH, frost);
        x += widths[2] + gap;
        BoardWidgets.stat(layer, overview.association(), x, top, widths[3], statH, NoticeBoardIcons.PIN_GREEN, -0.6f, frost, this::openTab);

        int lowerTop = top + statH + 10;
        int leftW = (int) (w * 0.58) - gap;
        int conditionsH = BoardWidgets.conditions(layer, t("conditions"), overview.conditions(), 0, lowerTop, leftW, frost);
        int researchTop = lowerTop + conditionsH + 8;
        int researchH = BoardWidgets.progress(layer, t("research.heading"), overview.research(), t("research.none"), 0, researchTop, leftW,
                h - researchTop, frost, () -> openTab(RESEARCH));
        int stripTop = researchTop + researchH + 10;
        List<Person> onDuty = view.researchers().people().stream().filter(p -> p.duty() == Duty.ON_DUTY).toList();
        if (h - stripTop >= 60)
            BoardWidgets.photoStrip(layer, t("overview.on_duty"), onDuty, 2, stripTop, leftW, person -> {
                selectedPerson = person.id();
                openTab(RESEARCHERS);
            });

        int stackX = leftW + gap + 4;
        noteStack(layer, stackX, lowerTop + 4, w - stackX - 6, h - lowerTop - 10);
    }

    // ------------------------------------------------------------------ researchers

    private void buildResearchers(UILayer layer) {
        header(layer, false);
        Researchers team = view.researchers();
        BoardWidgets.workers(layer, 0, 30, contentWidth(), contentHeight() - 30, team.headline(), team.people(), team.slots(), team.emptySlots(),
                selectedPerson, id -> {
                    selectedPerson = id;
                    refreshElements();
                }, t("researchers.none"), List.of(b("person.efficiency.tooltip")));
    }

    // ------------------------------------------------------------------ collaborative research

    private void buildResearch(UILayer layer) {
        header(layer, false);
        Research research = view.research();
        int w = contentWidth(), h = contentHeight();
        int top = 30;
        BoardWidgets.woodText(layer, research.headline(), 2, top, w - 4, research.headlineTooltip(), null);
        int listTop = top + 14, listH = h - listTop - 14;
        if (research.orders().isEmpty()) {
            PaperCard empty = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, research.empty(), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 4));
            empty.setPosAndSize(w / 8, listTop + 10, w * 3 / 4, 56);
            layer.add(empty);
        } else {
            ScrollColumn column = new ScrollColumn(layer, 8, scroll, c -> {
                float tilt = -0.3f;
                for (WorkOrder order : research.orders()) {
                    c.add(workOrder(c, order, w - 12, tilt));
                    tilt = -tilt;
                }
            }, value -> scroll = value);
            column.setPosAndSize(0, listTop, w, listH);
            layer.add(column);
        }
        if (!research.idle().getString().isEmpty())
            BoardWidgets.woodText(layer, research.idle(), 2, h - 10, w - 4, List.of(),
                    research.idleLinksAssociation() ? () -> openTab(ASSOCIATION) : null);
    }

    private PaperCard workOrder(UILayer parent, WorkOrder order, int w, float tilt) {
        boolean done = order.stamp() == OrderStamp.DONE;
        int h = done ? 20 : order.problem().getString().isEmpty() ? 40 : 50;
        int frost = sheetFrost();
        PaperCard card = new PaperCard(parent).tilt(tilt).frost(() -> frost)
                .pin(order.stamp() == OrderStamp.STOP ? NoticeBoardIcons.PIN_RED : NoticeBoardIcons.PIN_BLUE)
                .painter((g, x, y, cw, ch) -> {
                    int px = x + 8, py = y + 6;
                    int ink = order.stamp() == OrderStamp.STOP ? NoticeBoardTheme.RED : NoticeBoardTheme.INK;
                    if (done) {
                        BoardDraw.text(g, order.title(), px, py, cw - 70, NoticeBoardTheme.MUTED);
                        BoardDraw.stamp(g, b("stamp.done"), x + cw - 30, y + ch / 2, NoticeBoardTheme.GREEN_FILL, false, -6, 1);
                        return;
                    }
                    Component tag = t(order.required() ? "order.required" : "order.optional");
                    int titleW = cw - 16 - BoardDraw.chipWidth(tag) - 6 - BoardDraw.font().width(order.progress()) - 8;
                    int drawn = BoardDraw.text(g, order.title().copy().withStyle(ChatFormatting.BOLD), px, py, titleW, ink);
                    BoardDraw.chip(g, tag, px + drawn + 6, py - 1, order.required() ? NoticeBoardTheme.AMBER_INK : NoticeBoardTheme.MUTED);
                    BoardDraw.textRight(g, order.progress(), x + cw - 8, py, ink);
                    py += 12;
                    NoticeBoardTheme.INSTANCE.drawProgressBar(g, px, py, cw - 16, 5, order.percent() / 100f,
                            order.stamp() == OrderStamp.STOP ? NoticeBoardTheme.RED : order.percent() >= 50 ? NoticeBoardTheme.GREEN_FILL : NoticeBoardTheme.AMBER);
                    py += 9;
                    MutableComponent details = Component.empty();
                    for (int i = 0; i < order.details().size(); i++) {
                        if (i > 0) details.append(b("separator"));
                        details.append(order.details().get(i));
                    }
                    if (!order.today().getString().isEmpty())
                        details.append(b("separator")).append(order.today().copy().withStyle(ChatFormatting.DARK_GREEN));
                    BoardDraw.text(g, details, px, py, cw - 16, NoticeBoardTheme.MUTED);
                    if (!order.problem().getString().isEmpty())
                        BoardDraw.text(g, order.problem(), px, py + 11, cw - 70, order.stamp() == OrderStamp.STOP ? NoticeBoardTheme.RED : NoticeBoardTheme.AMBER);
                    if (order.stamp() == OrderStamp.SHORT)
                        BoardDraw.stamp(g, b("stamp.short"), x + cw - 32, y + ch - 14, NoticeBoardTheme.AMBER, false, -10, 1);
                    if (order.stamp() == OrderStamp.STOP)
                        BoardDraw.stamp(g, b("stamp.stop"), x + cw - 30, y + ch - 16, NoticeBoardTheme.RED, true, -12, 1);
                });
        card.setSize(w, h);
        card.setX(6);
        return card;
    }

    // ------------------------------------------------------------------ association

    private void buildAssociation(UILayer layer) {
        header(layer, false);
        Association association = view.association();
        int w = contentWidth(), h = contentHeight();
        int top = 30;
        BoardWidgets.woodText(layer, association.headline(), 2, top, w - 4, List.of(), null);
        int leftW = (int) (w * 0.56);
        int resultW = leftW - 8;
        boolean idea = association.outcome() == Outcome.IDEA;
        int resultH = 9 + (idea ? 12 + 20 : BoardDraw.wrappedHeight(association.title(), resultW - 18, 2) + 2)
                + (association.detail().getString().isEmpty() ? 0 : BoardDraw.wrappedHeight(association.detail(), resultW - 18, 3) + 2)
                + (association.action().getString().isEmpty() ? 0 : BoardDraw.wrappedHeight(association.action(), resultW - 30, 2)) + 9;
        resultH = Math.max(40, Math.min(resultH, h - top - 70));
        PaperCard result = new PaperCard(layer).kind(PaperCard.Kind.NOTE).taped().tilt(-0.6f).painter((g, x, y, cw, ch) -> {
            int px = x + 9, py = y + 9;
            Component title = association.title();
            if (idea) {
                BoardDraw.text(g, t("assoc.idea.title"), px, py, cw - 18, NoticeBoardTheme.AMBER);
                py += 12;
                if (BoardDraw.bigWidth(title) <= cw - 18) {
                    BoardDraw.big(g, title, px, py, NoticeBoardTheme.NOTE_INK);
                    py += 20;
                } else py += BoardDraw.wrapped(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 18, NoticeBoardTheme.NOTE_INK, 2) + 2;
                BoardDraw.stamp(g, b("stamp.new_idea"), x + cw - 30, y + 14, NoticeBoardTheme.RED, false, -10, 1);
            } else py += BoardDraw.wrapped(g, title.copy().withStyle(ChatFormatting.BOLD), px, py, cw - 18, NoticeBoardTheme.NOTE_INK, 2) + 2;
            if (!association.detail().getString().isEmpty())
                py += BoardDraw.wrapped(g, association.detail(), px, py, cw - 18, NoticeBoardTheme.NOTE_INK, 3) + 2;
            if (!association.action().getString().isEmpty())
                BoardDraw.wrapped(g, BoardWidgets.arrow(association.action()).copy().withStyle(ChatFormatting.BOLD), px, py, cw - 18,
                        NoticeBoardTheme.NOTE_INK, 2);
        });
        result.setPosAndSize(2, top + 18, resultW, resultH);
        layer.add(result);

        int below = top + 18 + resultH + 10;
        List<PastIdea> ideas = association.history();
        PastIdea open = selectedIdea == null ? null : ideas.stream().filter(i -> i.key().equals(selectedIdea)).findFirst().orElse(null);
        // An open journal excerpt takes the space under today's notice; the thinkers and tips come back when it is put away.
        if (open != null) excerpt(layer, open, 4, below, resultW - 4, h - below - 2);
        else if (!association.people().isEmpty()) {
            BoardWidgets.woodText(layer, association.idle(), 4, below, leftW - 8, List.of(), null);
            int px = 4;
            for (Person person : association.people()) {
                if (px + 40 > leftW) break;
                PhotoCard photo = new PhotoCard(layer, person, px % 2 == 0 ? -3f : 2f, () -> false, () -> {
                    selectedPerson = person.id();
                    openTab(RESEARCHERS);
                });
                photo.caption(PhotoCard.captionFor(person, association.people()));
                photo.setPosAndSize(px, below + 14, 36, 44);
                layer.add(photo);
                px += 42;
            }
            below += 64;
        }
        if (open == null) BoardWidgets.tips(layer, t("assoc.tips"), association.tips(), 4, below, resultW - 4, h - below - 2);

        int rightX = leftW + 6, rightW = w - rightX - 4;
        PaperCard chance = new PaperCard(layer).pin(NoticeBoardIcons.PIN_GREEN).tilt(0.5f).painter((g, x, y, cw, ch) -> {
            BoardDraw.text(g, t("assoc.chance").copy().withStyle(ChatFormatting.BOLD), x + 8, y + 8, cw - 16, NoticeBoardTheme.INK);
            int mx = x + 8, my = y + 21;
            for (int i = 0; i < 5; i++) {
                int color = i < association.chanceSteps() ? NoticeBoardTheme.GREEN_FILL : 0xFFD3D1C7;
                g.fill(mx + i * 9, my, mx + i * 9 + 7, my + 6, color);
            }
            BoardDraw.text(g, association.chance(), mx + 50, my - 1, cw - 66, NoticeBoardTheme.INK);
        }).tooltip(association::chanceTooltip);
        chance.setPosAndSize(rightX, top + 18, rightW, 36);
        layer.add(chance);
        PaperCard history = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(-0.4f).painter((g, x, y, cw, ch) -> {
            BoardDraw.text(g, association.historyTitle().copy().withStyle(ChatFormatting.BOLD), x + 8, y + 8, cw - 16, NoticeBoardTheme.INK);
            if (ideas.isEmpty()) BoardDraw.wrapped(g, t("assoc.history.none"), x + 8, y + 21, cw - 16, NoticeBoardTheme.MUTED, 3);
        });
        int historyTop = top + 18 + 36 + 10;
        int historyH = 21 + 6 + Math.max(2, ideas.size()) * 11
                + (int) ideas.stream().filter(item -> !item.from().getString().isEmpty()).count() * 12;
        historyH = Math.min(h - historyTop - 2, historyH);
        history.setPosAndSize(rightX, historyTop, rightW, historyH);
        layer.add(history);
        // Each idea is a line on the sheet: its name and day, where it came from in pencil; a click pins its journal page.
        int rowY = historyTop + 20;
        for (PastIdea past : ideas) {
            int rowH = 11 + (past.from().getString().isEmpty() ? 0 : 12);
            if (rowY + rowH > historyTop + historyH - 3) break;
            boolean chosen = past.key().equals(selectedIdea);
            HitArea row = new HitArea(layer, rightX + 4, rowY, rightW - 8, rowH);
            layer.add(row.painter((g, px, py, pw, ph) -> {
                if (chosen || row.isMouseOver()) g.fill(px, py, px + pw, py + ph, chosen ? 0x50FAC775 : 0x14000000);
                int dayW = past.when().getString().isEmpty() ? 0 : BoardDraw.font().width(past.when()) + 4;
                BoardDraw.text(g, past.title(), px + 4, py + 1, pw - 8 - dayW, NoticeBoardTheme.INK);
                if (dayW > 0) BoardDraw.text(g, past.when(), px + pw - dayW, py + 1, dayW, NoticeBoardTheme.MUTED);
                if (!past.from().getString().isEmpty()) BoardDraw.text(g, past.from(), px + 10, py + 12, pw - 14, NoticeBoardTheme.MUTED);
            }).tooltip(List.of(t(chosen ? "assoc.history.close" : "assoc.history.open"))).onClick(() -> {
                selectedIdea = chosen ? null : past.key();
                refreshElements();
            }));
            rowY += rowH;
        }
    }

    /**
     * 札记摘录：一张钉在板上的札记纸，写着想法的名字、哪天想到、从哪几样知识想到，以及札记里的描述。完整的札记在绘图台上翻看。
     * A journal excerpt pinned on the board: the idea's name, the day it was found, what it came from and what the journal
     * says about it. The full journal is read at a drawing desk.
     */
    private void excerpt(UILayer layer, PastIdea idea, int x, int y, int w, int maxH) {
        if (maxH < 44) return;
        PaperCard page = new PaperCard(layer).kind(PaperCard.Kind.NOTE).taped().tilt(0.5f).painter((g, cx, cy, cw, ch) -> {
            int px = cx + 9, py = cy + 8, right = cw - 18;
            BoardDraw.text(g, t("assoc.excerpt"), px, py, right, NoticeBoardTheme.MUTED);
            py += 11;
            py += BoardDraw.wrapped(g, idea.title().copy().withStyle(ChatFormatting.BOLD), px, py, right, NoticeBoardTheme.NOTE_INK, 2) + 1;
            if (!idea.when().getString().isEmpty())
                py += BoardDraw.wrapped(g, idea.when(), px, py, right, NoticeBoardTheme.MUTED, 1) + 1;
            if (!idea.from().getString().isEmpty())
                py += BoardDraw.wrapped(g, idea.from(), px, py, right, NoticeBoardTheme.MUTED, 2) + 3;
            int room = Math.max(0, (cy + ch - 14 - py) / BoardDraw.LINE);
            if (room > 0 && !idea.excerpt().getString().isEmpty())
                BoardDraw.wrapped(g, idea.excerpt(), px, py, right, NoticeBoardTheme.NOTE_INK, room);
        }).onClick(() -> {
            selectedIdea = null;
            refreshElements();
        }).tooltip(() -> List.of(t("assoc.history.close")));
        page.setPosAndSize(x, y, w, maxH);
        layer.add(page);
        // A link down the page opens the real journal at this idea, read-only.
        Component link = BoardWidgets.arrow(t("assoc.excerpt.open")).copy().withStyle(ChatFormatting.BOLD);
        int linkW = Math.min(w - 18, BoardDraw.font().width(link) + 4);
        HitArea open = new HitArea(layer, x + 9, y + maxH - 14, linkW, 11);
        layer.add(open.painter((g, px, py, pw, ph) -> {
            if (open.isMouseOver()) g.fill(px - 2, py - 1, px + pw, py + ph, 0x30FFFFFF);
            BoardDraw.text(g, link, px, py + 1, pw, NoticeBoardTheme.AMBER_INK);
        }).tooltip(List.of(t("assoc.excerpt.open.tooltip"))).onClick(() ->
                com.teammoeg.frostedresearch.knowledge.client.KnowledgeJournalViewer.open(idea.key())));
    }

    // ------------------------------------------------------------------ debug

    private void buildDebug(UILayer layer) {
        header(layer, false);
        List<BoardWidgets.DebugSection> sections = List.of(
                new BoardWidgets.DebugSection(t("debug.overview"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> overviewRows(menu))),
                new BoardWidgets.DebugSection(t("debug.researchers"), (l, x, y, w, h) -> new TownWorkforcePanel(
                        l, x, y,
                        menu::getResidents,
                        resident -> menu.getBuilding().map(building -> building.canResidentWork(resident)).orElse(false),
                        ResearchInstituteScreen::researchProficiency,
                        resident -> menu.getBuilding().map(building -> building.getResearcherLabor(resident)).orElse(0.0),
                        resident -> menu.getBuilding().map(building -> building.getResearcherLabor(resident)).orElse(0.0),
                        ResearchInstituteScreen::dailyProficiencyGain,
                        Component.translatable("gui.frostedheart.research_institute.research_proficiency"),
                        value -> Component.translatable("gui.frostedheart.research_institute.personal_labor", MineBaseScreen.two(value)),
                        resident -> List.of(Component.translatable("gui.frostedheart.research_institute.labor_tier",
                                education(ResearchInstituteDailyModel.tier(resident.getEducationLevel())))))),
                new BoardWidgets.DebugSection(t("debug.research"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> researchRows(menu))));
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), sections, debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, this::debugText);
    }

    private String debugText() {
        StringBuilder out = new StringBuilder();
        out.append(view.header().name().getString()).append('\n');
        for (TownInfoPanel.Row row : overviewRows(menu)) out.append(row.text().getString()).append('\n');
        out.append('\n');
        for (Resident resident : menu.getResidents()) {
            out.append(resident).append(": ")
                    .append(String.format(Locale.ROOT, "health %.2f mental %.2f strength %.2f intelligence %.2f education %d labor %.3f",
                            resident.getHealth(), resident.getMental(), resident.getStrength(), resident.getIntelligence(),
                            resident.getEducationLevel(), menu.getBuilding().map(b -> b.getResearcherLabor(resident)).orElse(0.0)))
                    .append('\n');
        }
        out.append('\n');
        for (TownInfoPanel.Row row : researchRows(menu)) out.append(row.text().getString()).append('\n');
        return out.toString();
    }

    // ------------------------------------------------------------------ original rows, now the debug view

    private static List<TownInfoPanel.Row> overviewRows(ResearchInstituteMenu menu) {
        ResearchInstituteBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return MineBaseScreen.missingBuildingRows();
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.research_institute.overview"));
        rows.add(MineBaseScreen.status("gui.frostedheart.town.workable", building.isBuildingWorkable()));
        if (!building.isBuildingWorkable()) {
            MineBaseScreen.appendCommonFailures(rows, building);
            if (building.getArea() < config.minimumFloorAreaBlocks.get())
                rows.add(MineBaseScreen.failure("gui.frostedheart.town.failure.area", building.getArea(), config.minimumFloorAreaBlocks.get()));
            if (building.getVolume() < config.minimumInteriorVolumeBlocks.get())
                rows.add(MineBaseScreen.failure("gui.frostedheart.town.failure.volume", building.getVolume(), config.minimumInteriorVolumeBlocks.get()));
            if (!building.isTemperatureValid())
                rows.add(MineBaseScreen.failure("gui.frostedheart.town.failure.temperature_low",
                        MineBaseScreen.one(building.getEffectiveTemperature()), MineBaseScreen.one(config.minimumWorkingTemperatureCelsius.get())));
            if (!building.isLightValid())
                rows.add(MineBaseScreen.failure("gui.frostedheart.research_institute.failure.light",
                        MineBaseScreen.one(building.getAverageLight()), MineBaseScreen.one(config.minimumAverageBlockLight.get())));
        }
        rows.add(MineBaseScreen.text("gui.frostedheart.town.workers_count", menu.getResidents().size(), building.getMaxResidents()));
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.desks", building.getDesks()));
        if (building.getDesks() > building.getMaxResidents())
            rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.space_limited",
                    MineBaseScreen.one(config.floorBlocksPerResearcher.get())), 0xFFFFAA00));
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.bookshelves", building.getBookshelves()));
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.decoration", percent(building.getDecorationRating()),
                percent(ResearchInstituteDailyModel.decorationMultiplier(building.getDecorationRating(), config.decorationBonusAtFullRating.get()) - 1)));
        rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.light",
                MineBaseScreen.one(building.getAverageLight()), MineBaseScreen.one(config.minimumAverageBlockLight.get())),
                building.isLightValid() ? 0xFF55FF55 : 0xFFFF5555));
        rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.temperature",
                MineBaseScreen.one(building.getEffectiveTemperature()), MineBaseScreen.one(config.minimumWorkingTemperatureCelsius.get())),
                building.isTemperatureValid() ? 0xFF55FF55 : 0xFFFF5555));
        rows.add(building.getTemperatureModifier() > 0
                ? MineBaseScreen.text("gui.frostedheart.hunting_base.heating_active", MineBaseScreen.one(building.getTemperatureModifier()))
                : MineBaseScreen.text("gui.frostedheart.hunting_base.heating_inactive"));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.area", building.getArea()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.volume", building.getVolume()));
        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.research_institute.capacity"));
        if (building.isBuildingWorkable())
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.capacity_granted", building.archiveCapacity(), building.inboxCapacity()));
        else rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.capacity_paused",
                ResearchInstituteDailyModel.capacity(building.getBookshelves(), config.archiveObservationsPerBookshelf.get()),
                ResearchInstituteDailyModel.capacity(building.getBookshelves(), config.inboxObservationsPerBookshelf.get())), 0xFFFFAA00));
        return rows;
    }

    private static List<TownInfoPanel.Row> researchRows(ResearchInstituteMenu menu) {
        ResearchInstituteBuilding building = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (building == null || town == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.town.next_settlement_forecast"));
        laborRows(rows, building.forecastLabor(town));

        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.town.last_settlement"));
        ResearchInstituteBuilding.InstituteDailyReport report = building.getDailyReport();
        if (!report.hasData()) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.town.no_production_report"), 0xFFAAAAAA));
        } else {
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.worked_researchers", report.workerCount()));
            laborRows(rows, report.labor());
            if (report.stopReason() != TownProductionStopReason.NONE) rows.add(MineBaseScreen.reason(report.stopReason()));
        }

        KnowledgeJournalData data = knowledge();
        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.research_institute.town_research"));
        KnowledgeJournalData.LaborDay day = data.laborDay;
        if (day == null) {
            rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.no_town_report"), 0xFFAAAAAA));
        } else if (day.total() <= 1e-6) {
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.town_labor", 0));
            rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.no_town_labor"), 0xFFAAAAAA));
        } else {
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.town_labor", ResearchInstituteDailyModel.shown(day.total())));
            if (day.allocations().isEmpty())
                rows.add(TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.no_requirements"), 0xFFAAAAAA));
            for (KnowledgeJournalData.Allocation allocation : day.allocations()) {
                Component title = allocation.title().getString().isBlank()
                        ? Component.translatable("gui.frostedheart.research_institute.requirement." + (allocation.type().isBlank() ? "unknown" : allocation.type()))
                        : allocation.title();
                rows.add(TownInfoPanel.Row.text(Component.translatable("gui.frostedheart.research_institute.allocation",
                        allocation.project(), title)));
                String key = allocation.given() <= 1e-6
                        ? allocation.itemsShort() ? "gui.frostedheart.research_institute.allocation_no_items" : "gui.frostedheart.research_institute.allocation_no_labor"
                        : allocation.itemsShort() ? "gui.frostedheart.research_institute.allocation_items_short" : "gui.frostedheart.research_institute.allocation_given";
                rows.add(TownInfoPanel.Row.colored(Component.translatable(key, ResearchInstituteDailyModel.workerDays(allocation.given()), ResearchInstituteDailyModel.workerDays(allocation.wanted())),
                        allocation.given() <= 1e-6 || allocation.itemsShort() ? 0xFFFFAA00 : 0xFFAAAAAA));
            }
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.idle_labor", ResearchInstituteDailyModel.shown(day.idleTotal())));
            rows.add(associationRow(day));
        }

        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.research_institute.knowledge_capacity"));
        KnowledgeJournalData.Capacity capacity = data.capacity;
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.archive_capacity", capacity.archiveUsed(), capacity.archiveLimit()));
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.inbox_capacity", capacity.inboxUsed(), capacity.inboxLimit()));
        return rows;
    }

    private static TownInfoPanel.Row associationRow(KnowledgeJournalData.LaborDay day) {
        String outcome = day.outcome().isBlank() ? "none" : day.outcome();
        String chance = percent(day.chance());
        return switch (outcome) {
            case "idea" -> TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.association.idea", day.subjectTitle()), 0xFF55FF55);
            case "hint" -> TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.association.hint", day.subjectTitle()), 0xFF55FF55);
            case "quiet" -> MineBaseScreen.text("gui.frostedheart.research_institute.association.quiet", chance);
            case "nothing" -> MineBaseScreen.text("gui.frostedheart.research_institute.association.nothing");
            default -> TownInfoPanel.Row.colored(Component.translatable("gui.frostedheart.research_institute.association.none",
                    education(FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.associationMinimumEducation.get())), 0xFFAAAAAA);
        };
    }

    private static void laborRows(List<TownInfoPanel.Row> rows, List<Double> labor) {
        double total = labor.stream().mapToDouble(Double::doubleValue).sum();
        rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.total_labor", ResearchInstituteDailyModel.shown(total)));
        for (int education = 0; education < labor.size(); education++) {
            if (labor.get(education) <= 1e-9) continue;
            rows.add(MineBaseScreen.text("gui.frostedheart.research_institute.tier_labor", education(education),
                    ResearchInstituteDailyModel.shown(labor.get(education))));
        }
    }

    /** The team's knowledge snapshot, decoded once per snapshot. */
    private static KnowledgeJournalData knowledge() {
        if (knowledge == null || knowledgeSerial != KnowledgeClientState.serial()) {
            knowledgeSerial = KnowledgeClientState.serial();
            knowledge = new KnowledgeJournalData(KnowledgeClientState.snapshot());
        }
        return knowledge;
    }

    private static Component education(int level) {
        return Component.translatable("gui.frostedheart.town_manager.education_level." + level);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", (Double.isFinite(value) ? value : 0) * 100);
    }

    private static double researchProficiency(Resident resident) {
        return resident.getWorkProficiency(ResearchInstituteBuilding.class);
    }

    private static double dailyProficiencyGain(Resident resident) {
        FHConfig.Server.Town.ResidentProgression progression = FHConfig.SERVER.TOWN.RESIDENT_PROGRESSION;
        return ResidentAttributeModel.calculateDailyProficiencyGain(
                resident.getWorkProficiency(ResearchInstituteBuilding.class),
                progression.proficiencyGrowthAtZeroPerWorkday.get(),
                progression.minimumProficiencyGrowthPerWorkday.get(),
                progression.maximumWorkProficiency.get());
    }
}
