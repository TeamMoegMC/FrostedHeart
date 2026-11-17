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

package com.teammoeg.frostedheart.item.townmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.network.TownBuildingRenameRequestPacket;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.client.TownUiModeClient;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardWidgets;
import com.teammoeg.frostedheart.content.town.client.board.FrostFade;
import com.teammoeg.frostedheart.content.town.client.board.HitArea;
import com.teammoeg.frostedheart.content.town.client.board.NoteStack;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardBuildingScreen;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.client.board.RenameBox;
import com.teammoeg.frostedheart.content.town.client.board.TabSlide;
import com.teammoeg.frostedheart.content.town.client.board.PaperCard;
import com.teammoeg.frostedheart.content.town.client.board.PhotoCard;
import com.teammoeg.frostedheart.content.town.client.board.PlaqueTab;
import com.teammoeg.frostedheart.content.town.client.board.ScrollColumn;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.network.TownNameEditRequestPacket;
import com.teammoeg.frostedheart.content.town.network.TownOperationalStatusRequestPacket;
import com.teammoeg.frostedheart.content.town.network.TownPolicyEditRequestPacket;
import com.teammoeg.frostedheart.content.town.network.TownResidentNameEditRequestPacket;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusClientCache;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.*;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter.BuildingCard;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter.EventLine;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter.Law;
import com.teammoeg.frostedheart.item.townmanager.view.TownHallPresenter.TownHallView;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 镇长印章：镇公所的布告板。概览、居民、建筑、法令和大事记是布告板上的纸；服务、岗位调度、住房和统计是挂在板上的黑板，
 * 沿用原来的面板和它们的拖拽、滑块；详细模式下多一页调试。页序与旧界面相同，旧的跳转仍然有效。
 * <p>
 * The Mayor's Seal: the town hall's notice board. Overview, residents, buildings, laws and events are paper on the
 * board; services, staffing, housing and statistics are chalkboards hung on it, keeping the original panels with
 * their drag and sliders; detailed mode adds a debug tab. Tabs keep their old order, so existing links still work.
 */
public class TownManagerScreen extends PrimaryLayer implements ITownDataUpdateListener, com.teammoeg.chorda.client.cui.base.HidesSidebarButtons {
    /** Native size of the original panels, which now hang on chalkboards. */
    public static final int FRAME_WIDTH = 264;
    public static final int FRAME_HEIGHT = 222;
    public static final int CONTENT_X = 8;
    public static final int CONTENT_Y = 6;
    public static final int CONTENT_WIDTH = FRAME_WIDTH - CONTENT_X * 2;
    public static final int CONTENT_HEIGHT = 204;
    public static final int OVERVIEW_TAB = 0, RESIDENTS_TAB = 1, BUILDINGS_TAB = 2, VIRTUAL_RESOURCES_TAB = 3, STAFFING_TAB = 4,
            HOUSING_TAB = 5, LAWS_TAB = 6, STATISTICS_TAB = 7, EVENTS_TAB = 8;
    private static final int PLAQUE_COLUMN = 30, PAD = 9;
    private static final Set<String> SEEN_ALERTS = new HashSet<>();

    private final List<NoticeBoardBuildingScreen.Tab> tabs = new ArrayList<>();
    private final List<Boolean> liveTabs = new ArrayList<>();
    private final UILayer content;
    @Nullable
    private final VirtualResourceType initialVirtualResource;
    /** The original pages, kept for the debug tab. */
    /** The original overview and event rows, kept for the debug sheets. */
    private final TownOverviewTab legacyOverview = new TownOverviewTab(this);
    private final TownEventsTab legacyEvents = new TownEventsTab(this);
    private int activeTab;
    private int statusRequestTicks, ticks, modeSerial = -1;
    private long lastStatusTime = Long.MIN_VALUE;
    private boolean dirty;
    private TownHallView view;
    /** The town hall's frost eases from what the mayor last saw; see {@link FrostFade}. */
    private final FrostFade frostFade = new FrostFade("town_hall");
    private final long openSeed = System.nanoTime();
    private List<Note> stack = List.of();
    private Set<String> fresh;
    /** UI state that survives rebuilds. */
    private int noteIndex, debugSection, peopleScroll, buildingScroll, eventScroll;
    private UUID selectedPerson;
    private boolean renamingTown, renamingResident;
    /** The building whose card shows a rename box, or null. */
    @Nullable
    private BlockPos renamingBuilding;

    public TownManagerScreen() {
        this(0);
    }

    public TownManagerScreen(int initialTab) {
        this(initialTab, null);
    }

    public TownManagerScreen(int initialTab, @Nullable VirtualResourceType initialVirtualResource) {
        super();
        this.initialVirtualResource = initialVirtualResource;
        this.activeTab = Math.max(0, Math.min(EVENTS_TAB, initialTab));
        setTheme(NoticeBoardTheme.INSTANCE);
        content = new UILayer(this) {
            @Override
            public void addUIElements() {
                if (activeTab >= 0 && activeTab < tabs.size()) tabs.get(activeTab).builder().accept(this);
            }

            @Override
            public void alignWidgets() {
            }
        };
        content.setScissorEnabled(false);
    }

    private static Component t(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownHallPresenter.KEY + key, args);
    }

    private static Component b(String key, Object... args) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.counted(TownFormat.KEY + key, args);
    }

    // ------------------------------------------------------------------ data the original panels read

    @Nullable
    public VirtualResourceType getInitialVirtualResource() {
        return initialVirtualResource;
    }

    @Nullable
    public TeamTownData getTownData() {
        return com.teammoeg.frostedheart.content.town.client.TownClientUpdates.currentTown().orElse(null);
    }

    @Nullable
    public TeamTown getTown() {
        TeamTownData data = getTownData();
        return data == null ? null : data.createTeamTown();
    }

    @Nullable
    public TownOperationalStatus getOperationalStatus() {
        return TownOperationalStatusClientCache.get();
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabs.size() || index == activeTab) return;
        tabSlide.leave(activeTab);
        activeTab = index;
        refreshElements();
    }

    /** The plaques slide out and back when the page changes. */
    private final TabSlide tabSlide = new TabSlide();

    // ------------------------------------------------------------------ the board

    private int contentWidth() {
        return getWidth() - PLAQUE_COLUMN - PAD * 2;
    }

    private int contentHeight() {
        return getHeight() - PAD * 2;
    }

    private void addTabs(boolean detailed) {
        tab(CIcons.getIcon(FHItems.town_manager.get()), t("tab.overview"), false, this::buildOverview, false);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.VILLAGER_SPAWN_EGG), t("tab.residents"), false, this::buildResidents, false);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.BRICKS), t("tab.buildings"), false, this::buildBuildings, false);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.HOPPER), t("tab.services"), false, page((l, w, h) ->
                new TownVirtualResourcesPanel(l, 0, 28, w, h - 28, this::getTown, initialVirtualResource)), true);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.CLOCK), t("tab.staffing"), false, page((l, w, h) ->
                new TownStaffingPanel(l, 0, 28, w, h - 28, this::getTown)), true);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.OAK_DOOR), t("tab.housing"), false, page((l, w, h) ->
                new TownHousingPanel(l, 0, 28, w, h - 28, this::getTown)), true);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.PAPER), t("tab.laws"), false, this::buildLaws, false);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.MAP), t("tab.statistics"), false, page((l, w, h) ->
                new TownStatisticsPanel(l, 0, 28, w, h - 28, this::getTownData)), true);
        tab(CIcons.getIcon(net.minecraft.world.item.Items.BELL), t("tab.events"), false, this::buildEvents, false);
        if (detailed) tab(CIcons.getIcon(net.minecraft.world.item.Items.SPYGLASS), b("detailed"), true, this::buildDebug, true);
    }

    private void tab(CIcons.CIcon icon, Component title, boolean debug, Consumer<UILayer> builder, boolean live) {
        tabs.add(new NoticeBoardBuildingScreen.Tab(icon, title, debug, builder));
        liveTabs.add(live);
    }

    @Override
    public boolean onInit() {
        resizeToWindow();
        TeamTownData.addClientListener(this);
        TownOperationalStatusClientCache.reset();
        requestOperationalStatus();
        return super.onInit();
    }

    private boolean resizeToWindow() {
        int guiW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int w = Math.max(260, Math.min(NoticeBoardBuildingScreen.MAX_W, guiW - 12 - PLAQUE_COLUMN)) + PLAQUE_COLUMN;
        int h = Math.max(200, Math.min(NoticeBoardBuildingScreen.MAX_H, guiH - 12));
        if (w == getWidth() && h == getHeight()) return false;
        setSize(w, h);
        return true;
    }

    @Override
    public void addChildUIElements() {
        tabs.clear();
        liveTabs.clear();
        modeSerial = TownUiModeClient.serial();
        addTabs(TownUiModeClient.detailed());
        if (activeTab >= tabs.size()) activeTab = 0;
        TeamTownData data = getTownData();
        view = TownHallPresenter.present(new TownHallPresenter.Inputs(getTown(), data, getOperationalStatus(), data == null ? 0 : data.getTownDay()));
        frostFade.track(view.header().frost());
        composeNotes();
        content.setPosAndSize(PLAQUE_COLUMN + PAD, PAD, contentWidth(), contentHeight());
        add(content);
        // Ten plaques must fit a short window too: they close up rather than run off the board.
        int step = Math.max(19, Math.min(27, (getHeight() - 14) / Math.max(1, tabs.size())));
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            NoticeBoardBuildingScreen.Tab tab = tabs.get(i);
            PlaqueTab plaque = new PlaqueTab(this, tab.icon(), tab.title(), tab.debug(), () -> activeTab == index, () -> {
                tabSlide.leave(activeTab);
                activeTab = index;
                renamingTown = renamingResident = false;
                renamingBuilding = null;
                refreshElements();
            }, () -> tabSlide.out(index, activeTab));
            plaque.setPosAndSize(4, 14 + i * step, 28, Math.min(24, step - 2));
            add(plaque);
        }
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void setSizeToContentSize() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        frostFade.begin();
        NoticeBoardTheme.drawBoard(graphics, x + PLAQUE_COLUMN, y, w - PLAQUE_COLUMN, h);
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (++statusRequestTicks >= 20) requestOperationalStatus();
        boolean rebuild = resizeToWindow() || modeSerial != TownUiModeClient.serial();
        // Live pages refresh themselves and must not be rebuilt under a drag; a rename in progress keeps its box.
        boolean live = activeTab < liveTabs.size() && liveTabs.get(activeTab);
        if (!live && !renamingTown && !renamingResident && renamingBuilding == null) {
            TownOperationalStatus status = getOperationalStatus();
            long time = status == null ? Long.MIN_VALUE : status.serverGameTime();
            if (time != lastStatusTime && ticks % 20 == 0) {
                lastStatusTime = time;
                rebuild = true;
            }
            if (dirty && ticks % 5 == 0) rebuild = true;
            if (ticks % 100 == 0) rebuild = true;
        }
        if (rebuild) {
            dirty = false;
            refreshElements();
        }
    }

    private void requestOperationalStatus() {
        statusRequestTicks = 0;
        FHNetwork.INSTANCE.sendToServer(new TownOperationalStatusRequestPacket());
    }

    @Override
    public void onClosed() {
        if (!stack.isEmpty()) markSeen(Math.floorMod(noteIndex, stack.size()));
        TeamTownData.removeClientListener(this);
        FrostFade.clear(frostFade);
        super.onClosed();
    }

    @Override
    public void onBuildingsChanged() {
        dirty = true;
    }

    @Override
    public void onResidentsChanged() {
        dirty = true;
    }

    @Override
    public void onResourcesChanged() {
        dirty = true;
    }

    // ------------------------------------------------------------------ notes

    private void composeNotes() {
        if (fresh == null) {
            fresh = new HashSet<>();
            for (Note note : view.notes()) if (!SEEN_ALERTS.contains(note.problem())) fresh.add(note.problem());
        }
        List<Note> talk = new ArrayList<>(view.talk());
        Collections.shuffle(talk, new Random(openSeed));
        List<Note> notes = new ArrayList<>(view.notes());
        notes.addAll(talk);
        if (stack.isEmpty() && view.notes().isEmpty() && !notes.isEmpty()) noteIndex = new Random(openSeed).nextInt(notes.size());
        stack = notes;
    }

    private void markSeen(int index) {
        if (index < 0 || index >= stack.size() || stack.get(index).kind() != NoteKind.IMPROVE) return;
        SEEN_ALERTS.add(stack.get(index).problem());
        if (fresh != null) fresh.remove(stack.get(index).problem());
    }

    private void header(UILayer layer, boolean tall) {
        BoardWidgets.header(layer, contentWidth(), tall, view.header(), CIcons.getIcon(FHItems.town_manager.get()),
                view.header().name().getString(), () -> 1f, TownHallPresenter.STATUS_KEY);
    }

    // ------------------------------------------------------------------ overview

    private void buildOverview(UILayer layer) {
        header(layer, true);
        int w = contentWidth(), h = contentHeight();
        if (view.stats().isEmpty()) return;
        renameTown(layer);
        int top = 38, gap = 6, statH = 46;
        int[] widths = BoardWidgets.widths(w, gap, 1, 1, 1, 1);
        CTextureIcon[] pins = {NoticeBoardIcons.PIN_RED, NoticeBoardIcons.PIN_BLUE, NoticeBoardIcons.PIN_GREEN, NoticeBoardIcons.PIN_BRASS};
        float[] tilts = {0.5f, -0.4f, 0.3f, -0.6f};
        int x = 0;
        for (int i = 0; i < view.stats().size(); i++) {
            BoardWidgets.stat(layer, view.stats().get(i), x, top, widths[i], statH, pins[i], tilts[i], 0, this::selectTab);
            x += widths[i] + gap;
        }
        int lowerTop = top + statH + 10;
        int leftW = (int) (w * 0.58) - gap;
        int conditionsH = BoardWidgets.conditions(layer, t("conditions"), view.conditions(), 0, lowerTop, leftW, 0);
        int towerTop = lowerTop + conditionsH + 8, towerH = 7 + 12 + view.tower().size() * 11 + 4;
        if (h - towerTop >= towerH) {
            PaperCard tower = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BLUE).tilt(-0.3f).painter((g, cx, cy, cw, ch) -> {
                BoardDraw.text(g, t("tower").copy().withStyle(ChatFormatting.BOLD), cx + 8, cy + 7, cw - 16, NoticeBoardTheme.INK);
                int py = cy + 19;
                // Each line has a small lamp lit in its tone, like the indicators on the tower itself.
                for (Chip line : view.tower()) {
                    int color = BoardDraw.toneColor(line.tone());
                    g.fill(cx + 9, py + 2, cx + 13, py + 6, line.tone() == Tone.MUTED ? 0xFFB4B2A9 : color);
                    g.fill(cx + 9, py + 2, cx + 13, py + 3, 0x40FFFFFF);
                    BoardDraw.text(g, line.text(), cx + 17, py, cw - 25, line.tone() == Tone.MUTED ? NoticeBoardTheme.MUTED : color);
                    py += 11;
                }
            });
            tower.setPosAndSize(2, towerTop, leftW - 4, towerH);
            layer.add(tower);
        }
        int stackX = leftW + gap + 4, stackW = w - stackX - 6;
        NoteStack notes = BoardWidgets.notes(layer, stack, noteIndex, fresh == null ? Set.of() : fresh, index -> noteIndex = index, this::markSeen,
                stackX, lowerTop + 4, stackW, h - lowerTop - 10 - 56);
        int recentTop = lowerTop + 4 + notes.getHeight() + 10;
        if (h - recentTop >= 40 && !view.events().isEmpty()) {
            List<EventLine> recent = view.events().subList(0, Math.min(2, view.events().size()));
            PaperCard slip = new PaperCard(layer).kind(PaperCard.Kind.GRID).pin(NoticeBoardIcons.PIN_GREEN).tilt(-0.5f).painter((g, cx, cy, cw, ch) -> {
                BoardDraw.text(g, t("recent").copy().withStyle(ChatFormatting.BOLD), cx + 8, cy + 7, cw - 16, NoticeBoardTheme.INK);
                int py = cy + 19;
                for (EventLine line : recent) {
                    BoardDraw.text(g, line.text(), cx + 8, py, cw - 16, BoardDraw.toneColor(line.tone()));
                    py += 11;
                }
            }).onClick(() -> selectTab(EVENTS_TAB));
            slip.setPosAndSize(stackX + 4, recentTop, stackW - 8, 19 + recent.size() * 11 + 4);
            layer.add(slip);
        }
    }

    /** A tag beside the town's name that turns it into a text box; Enter files the new name with the server. */
    private void renameTown(UILayer layer) {
        int w = contentWidth();
        Component current = view.header().name();
        if (!renamingTown) {
            BoardWidgets.renameTag(layer, w, current, t("rename.tooltip"), () -> {
                renamingTown = true;
                refreshElements();
            });
            return;
        }
        BoardWidgets.renameBox(layer, w, current.getString(), current.getString(), 32, name -> {
            renamingTown = false;
            if (!name.isBlank() && !name.equals(current.getString())) FHNetwork.INSTANCE.sendToServer(new TownNameEditRequestPacket(name));
            refreshElements();
        }, this::stopRenaming);
    }

    private void stopRenaming() {
        renamingTown = renamingResident = false;
        renamingBuilding = null;
        refreshElements();
    }

    // ------------------------------------------------------------------ residents

    private void buildResidents(UILayer layer) {
        header(layer, false);
        int w = contentWidth(), h = contentHeight(), top = 30;
        List<Person> people = view.people();
        // The same four words as the photo stamps, counted the same way as the overview's chips and staffing.
        var heads = com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters.headcount(people);
        Component headline = heads.young() > 0
                ? t("residents.headline.young", heads.total(), heads.onDuty(), heads.free(), heads.off(), heads.resting(), heads.young())
                : t("residents.headline", heads.total(), heads.onDuty(), heads.free(), heads.off(), heads.resting());
        BoardWidgets.woodText(layer, headline, 2, top, w - 4, List.of(), null);
        if (people.isEmpty()) {
            PaperCard empty = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, t("residents.none"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
            empty.setPosAndSize(w / 6, top + 20, w * 2 / 3, 50);
            layer.add(empty);
            return;
        }
        UUID chosen = people.stream().anyMatch(p -> p.id().equals(selectedPerson)) ? selectedPerson : people.get(0).id();
        Person person = people.stream().filter(p -> p.id().equals(chosen)).findFirst().orElse(people.get(0));
        int cardW = Math.min(BoardWidgets.PERSONNEL_MAX_W, w / 2 - 8), wallW = w - cardW - 16;
        int photoW = 48, photoH = 58, gapX = 8, columns = Math.max(1, (wallW + gapX) / (photoW + gapX));
        ScrollColumn wall = new ScrollColumn(layer, 10, peopleScroll, c -> {
            float[] tilts = {-3f, 2.5f, 1.5f, -2f, 2f, -1f};
            for (int i = 0; i < people.size(); i += columns) {
                List<Person> row = people.subList(i, Math.min(people.size(), i + columns));
                final int rowIndex = i;
                UILayer line = new UILayer(c) {
                    @Override
                    public void addUIElements() {
                        for (int j = 0; j < row.size(); j++) {
                            Person p = row.get(j);
                            PhotoCard photo = new PhotoCard(this, p, tilts[(rowIndex + j) % tilts.length], () -> p.id().equals(chosen), () -> {
                                selectedPerson = p.id();
                                renamingResident = false;
                                refreshElements();
                            });
                            photo.caption(PhotoCard.captionFor(p, view.people()));
                            photo.setPosAndSize(4 + j * (photoW + gapX), 4, photoW, photoH);
                            add(photo);
                        }
                    }

                    @Override
                    public void alignWidgets() {
                    }
                };
                line.setScissorEnabled(false);
                line.setSize(wallW, photoH + 6);
                c.add(line);
            }
        }, value -> peopleScroll = value);
        wall.setPosAndSize(0, top + 14, wallW, h - top - 14);
        layer.add(wall);

        int cardX = wallW + 12, cardH = Math.min(h - top - 18, BoardWidgets.personnelHeight(person, cardW) + 14);
        PaperCard card = new PaperCard(layer).pin(NoticeBoardIcons.PIN_BRASS).tilt(0.3f).frost(() -> Math.min(1, person.frost()))
                .painter((g, cx, cy, cw, ch) -> BoardWidgets.personnel(g, person, cx, cy, cw, ch - 14));
        card.setPosAndSize(cardX, top + 16, cardW, cardH);
        layer.add(card);
        if (!renamingResident) {
            Component label = t("rename");
            int tagW = BoardDraw.font().width(label) + 10;
            layer.add(new HitArea(layer, cardX + cardW - tagW - 8, top + 16 + cardH - 16, tagW, 12).painter((g, px, py, pw, ph) -> {
                NoticeBoardIcons.TAG.draw(g, px, py, pw, ph);
                BoardDraw.textCentered(g, label, px + pw / 2, py + 2, NoticeBoardTheme.INK);
            }).tooltip(List.of(t("rename.resident.tooltip"))).onClick(() -> {
                renamingResident = true;
                refreshElements();
            }));
        } else {
            RenameBox box = new RenameBox(layer, person.name(), 48, name -> {
                renamingResident = false;
                int space = name.indexOf(' ');
                String first = space < 0 ? name : name.substring(0, space).trim(), last = space < 0 ? "" : name.substring(space + 1).trim();
                if (!first.isEmpty() && !name.equals(person.name()))
                    FHNetwork.INSTANCE.sendToServer(new TownResidentNameEditRequestPacket(person.id(), first, last));
                refreshElements();
            }, this::stopRenaming);
            box.ghostText = person.name();
            box.setPosAndSize(cardX + 8, top + 16 + cardH - 18, cardW - 16, 13);
            layer.add(box);
            box.setFocused(true);
        }
    }

    // ------------------------------------------------------------------ buildings

    private void buildBuildings(UILayer layer) {
        header(layer, false);
        int w = contentWidth(), h = contentHeight(), top = 30;
        List<BuildingCard> cards = view.buildings();
        long stopped = cards.stream().filter(c -> c.status() == Status.STOPPED).count(), limited = cards.stream().filter(c -> c.status() == Status.LIMITED).count();
        BoardWidgets.woodText(layer, t("buildings.headline", cards.size(), limited, stopped), 2, top, w - 4, List.of(), null);
        if (cards.isEmpty()) {
            PaperCard empty = new PaperCard(layer).pin(NoticeBoardIcons.PIN_RED).painter((g, x, y, cw, ch) ->
                    BoardDraw.wrapped(g, t("buildings.none"), x + 10, y + 12, cw - 20, NoticeBoardTheme.INK, 3));
            empty.setPosAndSize(w / 6, top + 20, w * 2 / 3, 50);
            layer.add(empty);
            return;
        }
        int columns = w >= 420 ? 2 : 1, gap = 8, cardW = (w - gap * (columns - 1) - 4) / columns, cardH = 34;
        ScrollColumn column = new ScrollColumn(layer, 6, buildingScroll, c -> {
            for (int i = 0; i < cards.size(); i += columns) {
                List<BuildingCard> row = cards.subList(i, Math.min(cards.size(), i + columns));
                UILayer line = new UILayer(c) {
                    @Override
                    public void addUIElements() {
                        int x = 2;
                        float tilt = -0.25f;
                        for (BuildingCard card : row) {
                            add(buildingCard(this, card, x, cardW, cardH, tilt));
                            renameBuilding(this, card, x, cardW);
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
        }, value -> buildingScroll = value);
        column.setPosAndSize(0, top + 16, w, h - top - 16);
        layer.add(column);
    }

    /**
     * 建筑卡片名字后面的小铅笔：点开后名字变成输入框，回车改名，留空恢复成类型加编号。
     * The pencil after a building card's name: it turns the name into a text box; Enter renames, a blank name restores
     * type and number.
     */
    private void renameBuilding(UILayer row, BuildingCard card, int x, int w) {
        TeamTown town = getTown();
        AbstractTownBuilding building = town == null ? null : town.getTownBuildings().get(card.pos());
        if (building == null) return;
        String shown = card.name().getString();
        if (!card.pos().equals(renamingBuilding)) {
            int stampW = BoardDraw.chipWidth(card.stamp());
            int nameW = Math.min(BoardDraw.font().width(card.name().copy().withStyle(ChatFormatting.BOLD)), w - 44 - stampW);
            HitArea pencil = new HitArea(row, x + 28 + nameW + 4, 1 + 6, 10, 10);
            row.add(pencil.painter((g, px, py, pw, ph) -> BoardDraw.pencil(g, px + 1, py + 1, pencil.isMouseOver()))
                    .tooltip(List.of(b("rename.tooltip"))).onClick(() -> {
                        renamingTown = renamingResident = false;
                        renamingBuilding = card.pos();
                        refreshElements();
                    }));
            return;
        }
        boolean custom = building.getCustomName() != null;
        String fallback = TownBuildingLabels.defaultName(town.getTownBuildings(), card.pos(), building).getString();
        RenameBox box = new RenameBox(row, shown, TownNamingModel.MAX_BUILDING_NAME_LENGTH, name -> {
            renamingBuilding = null;
            if (!name.equals(shown) && !(name.isEmpty() && !custom))
                FHNetwork.INSTANCE.sendToServer(new TownBuildingRenameRequestPacket(card.pos(), name));
            refreshElements();
        }, this::stopRenaming);
        box.ghostText = fallback;
        box.setPosAndSize(x + 26, 1 + 4, Math.max(60, Math.min(160, w - 90)), 13);
        row.add(box);
        box.setFocused(true);
    }

    private PaperCard buildingCard(UILayer parent, BuildingCard card, int x, int w, int h, float tilt) {
        int color = BoardWidgets.statusColor(card.status());
        PaperCard paper = new PaperCard(parent).pin(card.status() == Status.STOPPED ? NoticeBoardIcons.PIN_RED : NoticeBoardIcons.PIN_BLUE).tilt(tilt)
                .painter((g, cx, cy, cw, ch) -> {
                    g.renderItem(card.icon(), cx + 7, cy + (ch - 16) / 2);
                    int stampW = BoardDraw.chipWidth(card.stamp());
                    BoardDraw.text(g, card.name().copy().withStyle(ChatFormatting.BOLD), cx + 28, cy + 7, cw - 44 - stampW, NoticeBoardTheme.INK);
                    BoardDraw.chip(g, card.stamp(), cx + cw - 8 - stampW, cy + 5, color);
                    BoardDraw.text(g, card.line(), cx + 28, cy + 19, cw - 36,
                            card.status() == Status.RUNNING ? NoticeBoardTheme.MUTED : card.status() == Status.LIMITED ? NoticeBoardTheme.AMBER : NoticeBoardTheme.RED);
                }).tooltip(() -> card.pos().equals(renamingBuilding) ? List.of() : List.of(card.name(), card.line(), t("buildings.visit")));
        paper.setPosAndSize(x, 1, w, h);
        return paper;
    }

    // ------------------------------------------------------------------ laws

    private void buildLaws(UILayer layer) {
        header(layer, false);
        TownHallPresenter.Laws laws = view.laws();
        int w = contentWidth(), h = contentHeight(), top = 30;
        BoardWidgets.woodText(layer, laws.title().copy().append(b("separator")).append(laws.state()), 2, top, w - 4, laws.help(), null);
        int columns = Math.max(1, laws.laws().size()), gap = 10, cardW = (w - gap * (columns - 1) - 8) / columns;
        // Decrees are as tall as their wording needs, so the tips below keep their room on a small screen.
        int wording = 0;
        for (Law law : laws.laws()) wording = Math.max(wording, BoardDraw.wrappedHeight(law.description(), cardW - 18, 5));
        int cardH = Math.max(64, Math.min(24 + wording + 22, Math.min(110, h - top - 80)));
        int x = 4;
        float tilt = -0.6f;
        for (Law law : laws.laws()) {
            boolean clickable = !laws.locked() && !law.active() && !law.pending();
            PaperCard decree = new PaperCard(layer).kind(PaperCard.Kind.NOTE).pin(law.active() ? NoticeBoardIcons.PIN_GREEN : NoticeBoardIcons.PIN_BRASS)
                    .tilt(tilt).painter((g, cx, cy, cw, ch) -> {
                        BoardDraw.text(g, law.name().copy().withStyle(ChatFormatting.BOLD), cx + 9, cy + 10, cw - 18, NoticeBoardTheme.NOTE_INK);
                        BoardDraw.wrapped(g, law.description(), cx + 9, cy + 24, cw - 18, NoticeBoardTheme.NOTE_INK, 5);
                        if (law.active())
                            BoardDraw.stamp(g, t("laws.active"), cx + cw - 26, cy + ch - 18, NoticeBoardTheme.GREEN_FILL, false, -10, 1);
                        else if (law.pending())
                            BoardDraw.stamp(g, t("laws.pending.stamp"), cx + cw - 28, cy + ch - 18, NoticeBoardTheme.AMBER, false, -10, 1);
                        else if (clickable)
                            BoardDraw.text(g, t("laws.enact"), cx + 9, cy + ch - 14, cw - 18, NoticeBoardTheme.AMBER_INK);
                    }).tooltip(() -> clickable ? List.of(t("laws.enact.tooltip")) : laws.help());
            if (clickable) decree.onClick(() -> FHNetwork.INSTANCE.sendToServer(new TownPolicyEditRequestPacket(law.law())));
            decree.setPosAndSize(x, top + 20, cardW, cardH);
            layer.add(decree);
            x += cardW + gap;
            tilt = -tilt * 0.8f;
        }
        BoardWidgets.tips(layer, t("laws.tips"), laws.help(), w / 4, top + 20 + cardH + 14, w / 2, h - top - cardH - 36);
    }

    // ------------------------------------------------------------------ events

    private void buildEvents(UILayer layer) {
        header(layer, false);
        int w = contentWidth(), h = contentHeight(), top = 30;
        List<EventLine> events = view.events();
        PaperCard ledger = new PaperCard(layer).kind(PaperCard.Kind.GRID).pin(NoticeBoardIcons.PIN_RED).painter((g, cx, cy, cw, ch) -> {
            BoardDraw.text(g, t("events.title").copy().withStyle(ChatFormatting.BOLD), cx + 9, cy + 7, cw - 18, NoticeBoardTheme.INK);
            if (events.isEmpty()) BoardDraw.wrapped(g, t("events.none"), cx + 12, cy + 26, cw - 24, NoticeBoardTheme.MUTED, 3);
        }).content(c -> {
            if (events.isEmpty()) return;
            ScrollColumn rows = new ScrollColumn(c, 1, eventScroll, col -> {
                for (EventLine line : events)
                    col.add(new HitArea(col, 0, 0, w - 30, 11).painter((g, px, py, pw, ph) -> {
                        int whenW = Math.max(70, BoardDraw.font().width(line.when()) + 8);
                        BoardDraw.text(g, line.when(), px, py, whenW, NoticeBoardTheme.MUTED);
                        BoardDraw.text(g, line.text(), px + whenW, py, pw - whenW, BoardDraw.toneColor(line.tone()));
                    }));
            }, value -> eventScroll = value);
            rows.setPosAndSize(9, 20, w - 22, h - top - 30);
            c.add(rows);
        });
        ledger.setPosAndSize(2, top + 2, w - 4, h - top - 4);
        layer.add(ledger);
    }

    // ------------------------------------------------------------------ chalkboards and debug

    @FunctionalInterface
    private interface PanelFactory {
        com.teammoeg.chorda.client.cui.base.UIElement create(UILayer layer, int w, int h);
    }

    /** A page holding one of the management panels, drawn on the board at the board's size. */
    private Consumer<UILayer> page(PanelFactory factory) {
        return layer -> {
            header(layer, false);
            layer.add(factory.create(layer, contentWidth(), contentHeight()));
        };
    }

    private void buildDebug(UILayer layer) {
        header(layer, false);
        // Each sheet keeps the raw figures the board pages round away, on graph paper like the buildings' debug tabs.
        List<BoardWidgets.DebugSection> sections = List.of(
                debugSheet(t("tab.overview"), legacyOverview::collectRows),
                debugSheet(t("tab.residents"), () -> TownHallDebugRows.residents(getTown())),
                debugSheet(t("tab.buildings"), () -> TownHallDebugRows.buildings(getTown())),
                debugSheet(t("tab.laws"), () -> TownHallDebugRows.laws(getTownData())),
                debugSheet(t("tab.events"), legacyEvents::rows));
        BoardWidgets.debug(layer, 28, contentWidth(), contentHeight(), sections, debugSection, section -> {
            debugSection = section;
            refreshElements();
        }, () -> view.header().name().getString() + "\n\n" + TownHallDebugRows.text(legacyOverview.collectRows())
                + "\n" + TownHallDebugRows.text(TownHallDebugRows.residents(getTown()))
                + "\n" + TownHallDebugRows.text(TownHallDebugRows.buildings(getTown()))
                + "\n" + TownHallDebugRows.text(TownHallDebugRows.laws(getTownData()))
                + "\n" + TownHallDebugRows.text(legacyEvents.rows()));
    }

    private static BoardWidgets.DebugSection debugSheet(Component title, java.util.function.Supplier<List<TownInfoPanel.Row>> rows) {
        return new BoardWidgets.DebugSection(title, (l, x, y, w, h) -> new TownInfoPanel(l, x, y, w, h, rows));
    }
}
