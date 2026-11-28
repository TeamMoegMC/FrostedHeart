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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.view.WarehouseViewPresenter;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.view.WarehouseViewPresenter.WarehouseView;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardBuildingScreen;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.client.board.PaperCard;
import com.teammoeg.frostedheart.content.town.network.TownOperationalStatusRequestPacket;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Stat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 仓库界面：库存页是一本账（全镇存货，可搜索、排序、存取）和旁边的背包；概览页讲容量、存粮和存得最多的东西；
 * 详细模式下多一页调试。背包格子的位置由 {@link WarehouseMenu} 固定，界面把背包纸片画在它们周围。
 * <p>
 * The warehouse screen. The stock tab is a ledger of the town's goods (search, sort, take and store) beside the
 * player's pack; the overview covers capacity, food and the most-stocked goods; detailed mode adds a debug tab. The
 * pack's slots are fixed by {@link WarehouseMenu}, and the screen draws the pack card around them.
 */
public class WarehouseScreen extends NoticeBoardBuildingScreen<WarehouseMenu> {
    private static final int STOCK = WarehouseViewPresenter.TAB_STOCK;
    private static final int PACK_W = 176, PACK_H = 102;
    private static final int STATUS_REQUEST_TICKS = 200;

    private WarehouseView view;
    private int statusTicks;
    private boolean opened;
    private VirtualItemGridElement grid;
    /** The search box survives rebuilds: a new one takes over the focus of the old. */
    private TextBox searchBox;
    private boolean refocusSearch;

    public WarehouseScreen(WarehouseMenu menu) {
        super(menu);
    }

    private static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(WarehouseViewPresenter.KEY + key, args);
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    @Override
    protected void addTabs(List<Tab> tabs, boolean detailed) {
        // 概览 first, as on every board.
        tabs.add(new Tab(CIcons.getIcon(FHBlocks.WAREHOUSE.get()), t("tab.overview"), false, this::buildOverview));
        tabs.add(new Tab(CIcons.getIcon(Items.CHEST), t("tab.stock"), false, this::buildStock));
        if (detailed) tabs.add(new Tab(CIcons.getIcon(Items.SPYGLASS), b("detailed"), true, this::buildDebug));
    }

    @Override
    public boolean onInit() {
        FHNetwork.INSTANCE.sendToServer(new TownOperationalStatusRequestPacket());
        // Storing is why people come, so a working warehouse opens on its stock and one that cannot store opens where it
        // explains why. Only on the first open: a resize keeps the tab the player chose.
        if (!opened) activeTab = menu.isStructureValid() ? WarehouseViewPresenter.TAB_STOCK : WarehouseViewPresenter.TAB_OVERVIEW;
        else if (!menu.isStructureValid()) activeTab = WarehouseViewPresenter.TAB_OVERVIEW;
        opened = true;
        return super.onInit();
    }

    @Override
    public void tick() {
        super.tick();
        if (++statusTicks % STATUS_REQUEST_TICKS == 0) FHNetwork.INSTANCE.sendToServer(new TownOperationalStatusRequestPacket());
    }

    @Override
    protected long dataSerial() {
        TownOperationalStatus status = TownOperationalStatusClientCache.get();
        return status == null ? 0 : status.serverGameTime();
    }

    @Override
    protected void beforeRebuild() {
        refocusSearch = searchBox != null && searchBox.isFocused();
        TeamTown town = menu.getTown().orElse(null);
        long day = TownClientUpdates.currentTown().map(TeamTownData::getTownDay).orElse(0L);
        TownOperationalStatus status = TownOperationalStatusClientCache.get();
        Double foodDays = status != null && status.foodReserveDays().available() ? status.foodReserveDays().value() : null;
        int population = status != null ? status.population() : town == null ? 0 : town.getAllResidents().size();
        view = WarehouseViewPresenter.present(new WarehouseViewPresenter.Inputs(menu.getBuilding().orElse(null), pos(), town,
                town == null ? "" : town.getName(), day, menu.getAllResources(), foodDays, population));
        composeNotes(view.notes(), view.talk());
        trackStatus(view.header().status());
        trackFrost(view.header().frost());
    }

    private void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, view.header(), CIcons.getIcon(FHBlocks.WAREHOUSE.get()),
                menu.getTown().map(TeamTown::getName).orElse(""), this::stampScale, WarehouseViewPresenter.STATUS_KEY);
    }

    // ------------------------------------------------------------------ stock

    /**
     * 房间不合格时的库存页：一张钉着的告示，说明存取暂停和怎么修，背包格子收起。
     * The stock tab while the room fails: a pinned notice saying storing and taking are paused and how to fix it, with
     * the pack slots put away.
     */
    private void closedStock(UILayer layer) {
        menu.setSlotVisible(false);
        header(layer, false);
        int w = contentWidth();
        PaperCard notice = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(-0.4f).painter((g, x, y, cw, ch) -> {
            // The text keeps clear of the stamp's column on the right.
            int textW = cw - 20 - 44;
            BoardDraw.text(g, t("closed.title").copy().withStyle(ChatFormatting.BOLD), x + 10, y + 10, textW, NoticeBoardTheme.INK);
            int used = BoardDraw.wrapped(g, t("closed.body"), x + 10, y + 24, textW, NoticeBoardTheme.INK, 3);
            BoardDraw.wrapped(g, BoardWidgets.arrow(t("closed.action")).copy().withStyle(ChatFormatting.BOLD), x + 10, y + 24 + used + 6, textW,
                    NoticeBoardTheme.AMBER_INK, 2);
            BoardDraw.stamp(g, t("closed.stamp"), x + cw - 28, y + 22, NoticeBoardTheme.RED, false, -10, 1);
        }).onClick(() -> openTab(WarehouseViewPresenter.TAB_OVERVIEW)).tooltip(() -> List.of(t("closed.tooltip")));
        notice.setPosAndSize(w / 6, 40, w * 2 / 3, 84);
        layer.add(notice);
    }

    private void buildStock(UILayer layer) {
        if (!menu.isStructureValid()) {
            closedStock(layer);
            return;
        }
        menu.setSlotVisible(true);
        header(layer, false);
        int w = contentWidth(), h = contentHeight();
        int packX = WarehouseMenu.PACK_X - 8 - CONTENT_X, packY = WarehouseMenu.PACK_Y - 20 - CONTENT_Y;

        // The pack never tilts: its cells must stay under the real slots.
        PaperCard pack = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BRASS).painter((g, x, y, cw, ch) -> {
            BoardDraw.text(g, t("pack").copy().withStyle(ChatFormatting.BOLD), x + 8, y + 7, cw - 16, NoticeBoardTheme.INK);
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 9; col++) NoticeBoardTheme.INSTANCE.drawSlot(g, x + 8 + col * 18, y + 20 + row * 18, 16, 16);
            int hotbar = y + 20 + (WarehouseMenu.HOTBAR_Y - WarehouseMenu.PACK_Y);
            g.fill(x + 8, hotbar - 3, x + cw - 8, hotbar - 2, 0xFFD3D1C7);
            for (int col = 0; col < 9; col++) NoticeBoardTheme.INSTANCE.drawSlot(g, x + 8 + col * 18, hotbar, 16, 16);
        });
        pack.setPosAndSize(packX, packY, PACK_W, PACK_H);
        layer.add(pack);

        int summaryY = packY + PACK_H + 8;
        if (h - summaryY >= 40) {
            WarehouseViewPresenter.Capacity capacity = view.capacity();
            PaperCard summary = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).tilt(0.4f).painter((g, x, y, cw, ch) -> {
                int px = x + 8, py = y + 8;
                BoardDraw.text(g, t("stat.stored"), px, py, 40, NoticeBoardTheme.MUTED);
                Component percent = Component.literal(capacity.percent() + "%");
                int barX = px + 42, barW = cw - 16 - 42 - BoardDraw.font().width(percent) - 6;
                NoticeBoardTheme.INSTANCE.drawProgressBar(g, barX, py + 2, barW, 5, capacity.percent() / 100f,
                        capacity.percent() >= WarehouseViewPresenter.NEARLY_FULL ? NoticeBoardTheme.RED
                                : capacity.percent() >= 70 ? NoticeBoardTheme.AMBER : NoticeBoardTheme.GREEN_FILL);
                BoardDraw.textRight(g, percent, x + cw - 8, py, NoticeBoardTheme.INK);
                py += 13;
                BoardDraw.text(g, t("stat.food"), px, py, 40, NoticeBoardTheme.MUTED);
                BoardDraw.text(g, view.foodLine(), px + 42, py, cw - 16 - 42, BoardDraw.toneColor(view.foodTone()));
            }).tooltip(() -> List.of(capacity.line(), t("stat.stored.tooltip")));
            summary.setPosAndSize(packX + 2, summaryY, PACK_W - 4, 34);
            layer.add(summary);
            int tipsY = summaryY + 34 + 10;
            BoardWidgets.tips(layer, t("tips"), view.tips(), packX + 4, tipsY, PACK_W - 8, h - tipsY - 2);
        }

        int ledgerX = packX + PACK_W + 10, ledgerW = w - ledgerX, ledgerY = packY, ledgerH = h - ledgerY - 14;
        boolean oneRow = ledgerW >= 270;
        int gridTop = oneRow ? 21 : 35;
        int cols = Math.max(3, (ledgerW - 14 - 14) / 18), rows = Math.max(2, (ledgerH - gridTop - 6) / 18);
        PaperCard ledger = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) -> {
            Component title = t("ledger").copy().withStyle(ChatFormatting.BOLD);
            int drawn = BoardDraw.text(g, title, x + 8, y + 7, cw - 16, NoticeBoardTheme.INK);
            BoardDraw.text(g, b("separator").copy().append(t("ledger.kinds", menu.getAllResources().size())), x + 8 + drawn, y + 7,
                    oneRow ? 80 : cw - 24 - drawn, NoticeBoardTheme.MUTED);
            if (menu.getAllResources().isEmpty())
                BoardDraw.wrapped(g, t("ledger.empty"), x + 14, y + gridTop + 12, cw - 28, NoticeBoardTheme.MUTED, 3);
            else if (menu.getResources().isEmpty())
                BoardDraw.wrapped(g, t("ledger.no_match", menu.getSearchText()), x + 14, y + gridTop + 12, cw - 28, NoticeBoardTheme.MUTED, 2);
        }).content(c -> {
            grid = new VirtualItemGridElement(c, 7, gridTop, cols, rows, menu::getResources) {
                @Override
                public void render(GuiGraphics graphics, int x, int y, int gw, int gh, com.teammoeg.chorda.client.RenderingHint hint) {
                    // An empty ledger shows its message instead of rows of empty cells.
                    if (!menu.getResources().isEmpty()) super.render(graphics, x, y, gw, gh, hint);
                }
            }.compactRows();
            c.add(grid);
            TextButton sort = new TextButton(c, menu.getSortMode().label(), CIcons.nop()) {
                @Override
                public void onClicked(MouseButton button) {
                    WarehouseSortMode next = menu.getSortMode().next();
                    menu.setSortMode(next);
                    setTitle(next.label());
                    grid.resetScroll();
                }

                @Override
                public void getTooltip(TooltipBuilder tooltip) {
                    tooltip.accept(Component.translatable("gui.frostedheart.warehouse.sort.tooltip"));
                }
            };
            int sortW = BoardDraw.font().width(menu.getSortMode().label()) + 12;
            TextBox search = new TextBox(c) {
                @Override
                public void onTextChanged() {
                    menu.setSearchText(getText());
                    grid.resetScroll();
                }
            };
            search.ghostText = Component.translatable("gui.frostedheart.warehouse.search_hint").getString();
            search.setMaxLength(64);
            search.setText(menu.getSearchText(), false);
            int searchW = Math.min(110, ledgerW - 16 - sortW - 4);
            int rowY = oneRow ? 4 : 18;
            search.setPosAndSize(ledgerW - 8 - sortW - 4 - searchW, rowY, searchW, 13);
            sort.setPos(ledgerW - 8 - sortW, rowY - 1);
            c.add(search);
            c.add(sort);
            searchBox = search;
            if (refocusSearch) search.setFocused(true);
        });
        ledger.setPosAndSize(ledgerX, ledgerY, ledgerW, ledgerH);
        layer.add(ledger);
        BoardWidgets.woodText(layer, t("hint"), ledgerX + 2, h - 10, ledgerW - 4, view.tips(), null);
    }

    // ------------------------------------------------------------------ overview

    private void buildOverview(UILayer layer) {
        menu.setSlotVisible(false);
        header(layer, true);
        renameTag(layer, view.header().name());
        int w = contentWidth(), h = contentHeight();
        int top = 38, gap = 6, statH = 46;
        List<Stat> stats = view.stats();
        if (stats.isEmpty()) {
            PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, b("reason.missing"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
            card.setPosAndSize(w / 6, top + 10, w * 2 / 3, 44);
            layer.add(card);
            return;
        }
        int[] widths = BoardWidgets.widths(w, gap, 1.15, 1, 1, 0.85);
        var pins = new com.teammoeg.chorda.client.icon.CIcons.CTextureIcon[]{NoticeBoardIcons.PIN_RED, NoticeBoardIcons.PIN_GREEN,
                NoticeBoardIcons.PIN_BLUE, NoticeBoardIcons.PIN_BRASS};
        float[] tilts = {0.4f, -0.5f, 0.3f, -0.3f};
        int x = 0;
        for (int i = 0; i < stats.size(); i++) {
            BoardWidgets.stat(layer, stats.get(i), x, top, widths[i], statH, pins[i], tilts[i], 0, this::openTab);
            x += widths[i] + gap;
        }
        int lowerTop = top + statH + 10;
        int leftW = (int) (w * 0.58) - gap;
        int conditionsH = BoardWidgets.conditions(layer, t("conditions"), view.conditions(), 0, lowerTop, leftW, 0);
        int itemsTop = lowerTop + conditionsH + 8;
        if (h - itemsTop >= 40)
            BoardWidgets.items(layer, t("top"), view.topItems(), t("top.none"), 2, itemsTop, leftW - 4, h - itemsTop - 2,
                    NoticeBoardIcons.PIN_BLUE, -0.3f, 0, PaperCard.Kind.GRID);
        int stackX = leftW + gap + 4;
        noteStack(layer, stackX, lowerTop + 4, w - stackX - 6, h - lowerTop - 10);
    }

    // ------------------------------------------------------------------ debug

    private void buildDebug(UILayer layer) {
        menu.setSlotVisible(false);
        header(layer, false);
        List<BoardWidgets.DebugSection> sections = List.of(
                new BoardWidgets.DebugSection(t("debug.overview"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, this::overviewRows)),
                new BoardWidgets.DebugSection(t("debug.stock"), (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, this::stockRows)));
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), sections, debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, this::debugText);
    }

    private List<TownInfoPanel.Row> overviewRows() {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        WarehouseBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return MineBaseScreen.missingBuildingRows();
        rows.add(MineBaseScreen.title("gui.frostedheart.warehouse.overview"));
        rows.add(MineBaseScreen.status("gui.frostedheart.town.workable", building.isBuildingWorkable()));
        if (!building.isBuildingWorkable()) MineBaseScreen.appendCommonFailures(rows, building);
        rows.add(TownInfoPanel.Row.text(t("debug.capacity", decimal(building.getCapacity()), building.getArea(), building.getVolume(),
                building.getDecorationAmount())));
        TeamTown town = menu.getTown().orElse(null);
        if (town != null) {
            double limit = town.getResourceHolder().get(VirtualResourceType.MAX_CAPACITY.generateAttribute(0));
            rows.add(TownInfoPanel.Row.text(t("debug.town_limit", decimal(limit), decimal(town.getResourceHolder().getCapacityLeft()))));
        }
        TownOperationalStatus status = TownOperationalStatusClientCache.get();
        rows.add(status == null ? TownInfoPanel.Row.colored(t("debug.status_missing"), 0xFFAAAAAA)
                : TownInfoPanel.Row.text(t("debug.food", status.foodReserveDays().available() ? decimal(status.foodReserveDays().value()) : "-",
                status.population())));
        rows.add(TownInfoPanel.Row.text(t("debug.kinds", menu.getAllResources().size())));
        return rows;
    }

    private List<TownInfoPanel.Row> stockRows() {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        menu.getAllResources().stream().sorted(Comparator.comparingLong(VirtualItemStack::getAmount).reversed())
                .forEach(stack -> rows.add(TownInfoPanel.Row.text(stack.getDisplayStack().getHoverName().copy()
                        .append(Component.literal("  " + stack.getAmount()).withStyle(ChatFormatting.AQUA)))));
        if (rows.isEmpty()) rows.add(TownInfoPanel.Row.colored(t("ledger.empty"), 0xFFAAAAAA));
        return rows;
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String debugText() {
        StringBuilder out = new StringBuilder();
        out.append(view.header().name().getString()).append(" @ ").append(pos().toShortString()).append('\n');
        for (TownInfoPanel.Row row : overviewRows()) out.append(row.text().getString()).append('\n');
        out.append('\n');
        for (TownInfoPanel.Row row : stockRows()) out.append(row.text().getString()).append('\n');
        return out.toString();
    }
}
