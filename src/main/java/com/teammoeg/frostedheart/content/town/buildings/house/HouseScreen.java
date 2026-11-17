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

package com.teammoeg.frostedheart.content.town.buildings.house;

import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter.HouseView;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter.Meal;
import com.teammoeg.frostedheart.content.town.buildings.house.view.HouseViewPresenter.Residents;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardBuildingScreen;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.client.board.NoteStack;
import com.teammoeg.frostedheart.content.town.client.board.PaperCard;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.resident.ResidentNutrition;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Bar;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Person;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Stat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 住宅界面：布告板主题的概览、居民、餐食三页；详细模式下多一页调试，内容是原来的三个面板。
 * 显示内容由 {@link HouseViewPresenter} 算出，摆件来自 {@link BoardWidgets}。
 * <p>
 * The house screen in the notice-board theme: overview, residents and meals, plus a debug tab in detailed mode
 * holding the three original panels. {@link HouseViewPresenter} decides what is shown; {@link BoardWidgets} supplies
 * the pieces.
 */
public class HouseScreen extends NoticeBoardBuildingScreen<HouseMenu> {
    private static final int RESIDENTS = HouseViewPresenter.TAB_RESIDENTS;
    private static final CTextureIcon[] STAT_PINS = {NoticeBoardIcons.PIN_RED, NoticeBoardIcons.PIN_BLUE, NoticeBoardIcons.PIN_GREEN,
            NoticeBoardIcons.PIN_BRASS};
    private static final float[] STAT_TILTS = {0.5f, -0.4f, 0.3f, -0.6f};

    private HouseView view;

    public HouseScreen(HouseMenu menu) {
        super(menu);
    }

    private static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(HouseViewPresenter.KEY + key, args);
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    @Override
    protected void addTabs(List<Tab> tabs, boolean detailed) {
        tabs.add(new Tab(CIcons.getIcon(FHBlocks.HOUSE.get()), t("tab.overview"), false, this::buildOverview));
        tabs.add(new Tab(CIcons.getIcon(Items.RED_BED), t("tab.residents"), false, this::buildResidents));
        tabs.add(new Tab(CIcons.getIcon(Items.BREAD), t("tab.meal"), false, this::buildMeal));
        if (detailed) tabs.add(new Tab(CIcons.getIcon(Items.SPYGLASS), b("detailed"), true, this::buildDebug));
    }

    @Override
    protected void beforeRebuild() {
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        view = HouseViewPresenter.present(new HouseViewPresenter.Inputs(menu.getHouse().orElse(null), pos(), menu.getResidents(), town,
                town == null ? "" : town.getName(), day));
        composeNotes(view.notes(), view.talk());
        trackStatus(view.header().status());
        trackFrost(view.header().frost());
    }

    private void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, view.header(), CIcons.getIcon(FHBlocks.HOUSE.get()),
                menu.getTown().map(TeamTown::getName).orElse(""), this::stampScale, HouseViewPresenter.STATUS_KEY);
    }

    private int sheetFrost() {
        return BoardWidgets.sheetFrost(view.header().frost());
    }

    // ------------------------------------------------------------------ overview

    private void buildOverview(UILayer layer) {
        header(layer, true);
        renameTag(layer, view.header().name());
        int w = contentWidth(), h = contentHeight();
        int top = 38, gap = 6, statH = 46, frost = sheetFrost();
        List<Stat> stats = view.stats();
        if (stats.isEmpty()) {
            missing(layer, top);
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
        int conditionsH = BoardWidgets.conditions(layer, t("conditions"), view.conditions(), 0, lowerTop, leftW, frost);
        int stripTop = lowerTop + conditionsH + 10;
        List<Person> people = view.residents().people();
        // A house is its people: a full-size photo row when there is room, the small strip otherwise.
        boolean big = h - stripTop >= 14 + 58 + 4;
        if (h - stripTop >= 60)
            BoardWidgets.photoStrip(layer, t("overview.living_here"), people, 2, stripTop, leftW, big ? 48 : 36, big ? 58 : 44, person -> {
                selectedPerson = person.id();
                openTab(RESIDENTS);
            });
        int stackX = leftW + gap + 4, stackW = w - stackX - 6;
        NoteStack stack = noteStack(layer, stackX, lowerTop + 4, stackW, h - lowerTop - 10 - 52);
        int slipTop = lowerTop + 4 + stack.getHeight() + 10;
        // The last meal as a slip of squared paper; the percentage already sits on the stat note above.
        Meal meal = view.meal();
        if (h - slipTop >= BoardWidgets.SLIP_H)
            BoardWidgets.itemSlip(layer, t("overview.last_meal"), meal.items(),
                    view.residents().people().isEmpty() ? t("overview.last_meal.nobody")
                            : meal.satisfaction() < 0 ? t("overview.last_meal.none") : t("overview.last_meal.nothing"), stackX + 4, slipTop, stackW - 8,
                    () -> openTab(HouseViewPresenter.TAB_MEAL));
    }

    private void missing(UILayer layer, int top) {
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                BoardDraw.wrapped(g, b("reason.missing"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
        card.setPosAndSize(contentWidth() / 6, top + 10, contentWidth() * 2 / 3, 44);
        layer.add(card);
    }

    // ------------------------------------------------------------------ residents

    private void buildResidents(UILayer layer) {
        header(layer, false);
        Residents residents = view.residents();
        BoardWidgets.workers(layer, 0, 30, contentWidth(), contentHeight() - 30, residents.headline(), residents.people(), residents.slots(),
                residents.emptySlots(), selectedPerson, id -> {
                    selectedPerson = id;
                    refreshElements();
                }, residents.empty(), List.of());
    }

    // ------------------------------------------------------------------ meal

    private void buildMeal(UILayer layer) {
        header(layer, false);
        Meal meal = view.meal();
        int w = contentWidth(), h = contentHeight();
        int top = 34, frost = sheetFrost();
        int leftW = (int) (w * 0.56);
        BoardWidgets.items(layer, meal.title(), meal.items(), meal.empty(), 2, top, leftW - 8, h - top - 4, NoticeBoardIcons.PIN_BLUE, -0.4f,
                frost, PaperCard.Kind.GRID);

        int rightX = leftW + 6, rightW = w - rightX - 4;
        int fedH = meal.satisfaction() < 0 ? 0 : 62;
        if (fedH > 0) {
            PaperCard fed = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(0.5f).frost(() -> frost).painter((g, x, y, cw, ch) -> {
                BoardDraw.text(g, t("stat.fed"), x + 8, y + 7, cw - 16, NoticeBoardTheme.MUTED);
                BoardDraw.big(g, Component.literal(meal.satisfaction() + "%"), x + 8, y + 18, NoticeBoardTheme.INK);
                NoticeBoardTheme.INSTANCE.drawProgressBar(g, x + 8, y + 38, cw - 16, 5, meal.satisfaction() / 100f, BoardDraw.toneFill(meal.tone()));
                BoardDraw.text(g, meal.summary(), x + 8, y + 48, cw - 16, BoardDraw.toneColor(meal.tone()));
            }).tooltip(() -> List.of(t("stat.fed.tooltip")));
            fed.setPosAndSize(rightX, top, rightW, fedH);
            layer.add(fed);
        }
        int tipsTop = top + (fedH > 0 ? fedH + 10 : 0);
        BoardWidgets.tips(layer, t("meal.tips"), meal.tips(), rightX + 2, tipsTop, rightW - 4, h - tipsTop - 2);

        // Under the menu: what the meals add up to, the residents' average reserves.
        int menuH = Math.min(h - top - 4, 7 + 12 + Math.max(1, meal.items().size()) * 18 + 4);
        int reservesTop = top + menuH + 10, reservesH = 7 + 12 + 4 * 11 + 4 + 12;
        if (!meal.nutrition().isEmpty() && h - reservesTop >= reservesH) {
            PaperCard reserves = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BRASS).tilt(0.3f).frost(() -> frost).painter((g, x, y, cw, ch) -> {
                BoardDraw.text(g, t("meal.nutrition").copy().withStyle(ChatFormatting.BOLD), x + 8, y + 7, cw - 16, NoticeBoardTheme.INK);
                int py = y + 20, labelW = BoardWidgets.labelColumn(meal.nutrition(), cw - 16), valueW = 18;
                for (Bar bar : meal.nutrition()) {
                    BoardDraw.text(g, bar.label(), x + 8, py, labelW, NoticeBoardTheme.INK);
                    NoticeBoardTheme.INSTANCE.drawProgressBar(g, x + 8 + labelW, py + 2, cw - 16 - labelW - valueW - 4, 5, bar.value() / 100f,
                            BoardDraw.toneFill(bar.tone()));
                    BoardDraw.textRight(g, Component.literal(Integer.toString(bar.value())), x + cw - 8, py, NoticeBoardTheme.INK);
                    py += 11;
                }
                BoardDraw.text(g, meal.nutritionLine(), x + 8, py + 3, cw - 16, NoticeBoardTheme.MUTED);
            }).tooltip(() -> List.of(t("meal.nutrition.tooltip")));
            reserves.setPosAndSize(4, reservesTop, leftW - 14, reservesH);
            layer.add(reserves);
        }
    }

    // ------------------------------------------------------------------ debug

    private void buildDebug(UILayer layer) {
        header(layer, false);
        List<BoardWidgets.DebugSection> sections = List.of(
                new BoardWidgets.DebugSection(t("debug.overview"), (l, x, y, w, h) -> new HouseOverviewElement(l, x, y, menu)),
                new BoardWidgets.DebugSection(t("debug.meal"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> HouseDebugRows.meal(menu))),
                new BoardWidgets.DebugSection(t("debug.residents"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, () -> HouseDebugRows.residents(menu))));
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), sections, debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, this::debugText);
    }

    private String debugText() {
        StringBuilder out = new StringBuilder();
        out.append(view.header().name().getString()).append(" @ ").append(pos().toShortString()).append('\n');
        HouseBuilding house = menu.getHouse().orElse(null);
        if (house != null) {
            out.append(String.format(Locale.ROOT, "workable %s, structure %s, area %d, volume %d, beds %d, capacity %d%n",
                    house.isBuildingWorkable(), house.isStructureValid(), house.getArea(), house.getVolume(), house.getBedCount(), house.getMaxResidents()));
            out.append(String.format(Locale.ROOT, "temperature %.2f (heating %.2f), rating %.3f, space %.3f, decoration %.3f, comfort %.3f%n",
                    house.getEffectiveTemperature(), house.getTemperatureModifier(), house.getTemperatureRating(), house.getSpaceRating(),
                    house.getDecorationRating(), house.getComfortRating()));
            HouseBuilding.DailyReport report = house.getDailyReport();
            out.append(String.format(Locale.ROOT, "report %s, residents %d, food %.3f / %.3f, satisfaction %.3f, day %d%n", report.hasData(),
                    report.residentCount(), report.foodConsumed(), report.foodRequired(), report.foodSatisfaction(), report.meal().settlementDay()));
            for (HouseBuilding.MealEntry entry : report.meal().entries())
                out.append("  ").append(entry.item().toItemStack().getHoverName().getString())
                        .append(String.format(Locale.ROOT, " %.3f%n", entry.amount()));
        }
        out.append('\n');
        for (Resident resident : menu.getResidents()) {
            ResidentNutrition n = resident.getNutrition();
            out.append(resident).append(String.format(Locale.ROOT,
                    ": age %d, education %d, health %.2f, mental %.2f, strength %.2f, intelligence %.2f, carbohydrate %.1f, protein %.1f, vegetable %.1f, fat %.1f, work %s%n",
                    resident.getAge(), resident.getEducationLevel(), resident.getHealth(), resident.getMental(), resident.getStrength(),
                    resident.getIntelligence(), n.carbohydrate(), n.protein(), n.vegetable(), n.fat(),
                    resident.getWorkPos() == null ? "-" : resident.getWorkPos().toShortString()));
        }
        return out.toString();
    }
}
