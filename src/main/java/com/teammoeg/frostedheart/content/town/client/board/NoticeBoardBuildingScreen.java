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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownNamingModel;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import com.teammoeg.frostedheart.content.town.network.TownBuildingRenameRequestPacket;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.client.TownUiModeClient;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Note;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.NoteKind;
import com.teammoeg.frostedheart.content.town.ui.view.BoardView.Status;
import com.teammoeg.chorda.client.CInputHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 布告板风格的城镇建筑界面基类：木板随窗口缩放（最宽 560×340），左缘是标签木牌，
 * 详细模式下多一块红色的调试木牌。城镇数据、知识快照或详细模式变化时重建内容，选中的页、便签、居民等界面状态保存在字段里。
 * <p>
 * Base of the notice-board town building screens. The board follows the window between 387×228 and 560×340
 * pixels; tab plaques hang on its left edge, with a red debug plaque in detailed mode. Content is rebuilt when the
 * town data, the knowledge snapshot or the mode changes; UI state such as the selected tab lives in fields so it
 * survives rebuilds.
 */
public abstract class NoticeBoardBuildingScreen<C extends CBlockEntityMenu<? extends AbstractTownBuildingBlockEntity>>
        extends MenuPrimaryLayer<C> implements ITownDataUpdateListener, com.teammoeg.chorda.client.cui.base.HidesSidebarButtons {
    public static final int MIN_W = 387, MAX_W = 560, MIN_H = 228, MAX_H = 340;
    /** Board padding around the content layer. */
    public static final int PAD = 9;
    private static final int PLAQUE_COLUMN = 30;
    /** Where the content layer starts inside the screen layer, for menus whose slots must line up with the board. */
    public static final int CONTENT_X = PLAQUE_COLUMN + PAD, CONTENT_Y = PAD;

    /** A tab: its plaque icon and title, whether it is the debug tab, and what it builds into the content layer. */
    public record Tab(CIcon icon, Component title, boolean debug, Consumer<UILayer> builder) {
    }

    private final List<Tab> tabs = new ArrayList<>();
    private final UILayer content;
    protected int activeTab;
    private int modeSerial = -1;
    private boolean dirty;
    private int ticks;

    /** Problem kinds each building's notes have already shown, for the 新 stamp; kept for the game session. */
    private static final Map<BlockPos, Set<String>> SEEN_PROBLEMS = new HashMap<>();
    /** Last status stamp seen per building, so the stamp thumps down when it changes. */
    private static final Map<BlockPos, Status> SEEN_STATUS = new HashMap<>();
    private final long openSeed = System.nanoTime();
    private Set<String> fresh;
    private List<Note> stack = List.of();
    private long stampAt = -1;
    /** Whether the header's name is being edited; rebuilds wait until it is done. */
    protected boolean renaming;
    /** UI state that survives rebuilds. */
    protected int noteIndex;
    protected UUID selectedPerson;
    protected int debugSection;
    protected int scroll;

    protected NoticeBoardBuildingScreen(C menu) {
        super(menu);
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

    /** Adds the screen's tabs; the debug tab is only listed in detailed mode. */
    protected abstract void addTabs(List<Tab> tabs, boolean detailed);

    /** Called before every rebuild, so subclasses can recompute their view. */
    protected void beforeRebuild() {
    }

    /** Changes whenever the screen's external data does, so the screen rebuilds; 0 when it has none. */
    protected long dataSerial() {
        return 0;
    }

    public int contentWidth() {
        return getWidth() - PLAQUE_COLUMN - PAD * 2;
    }

    public int contentHeight() {
        return getHeight() - PAD * 2;
    }

    /** Opens the tab at {@code index}, for links between pages. */
    public void openTab(int index) {
        if (index < 0) return;
        tabSlide.leave(activeTab);
        activeTab = index;
        refreshElements();
    }

    /** The plaques slide out and back when the page changes. */
    private final TabSlide tabSlide = new TabSlide();

    public BlockPos pos() {
        return menu.getBlock().getBlockPos();
    }

    /**
     * 组成便签叠：改进便签按给定顺序在前，留言按本次打开固定的随机顺序在后；没有改进时从随机一张开始。
     * Builds the note stack: improvements first in their given order, then small talk in an order fixed for this
     * opening; with nothing to improve the stack opens at a random note.
     */
    protected void composeNotes(List<Note> improvements, List<Note> talk) {
        if (fresh == null) {
            Set<String> seen = SEEN_PROBLEMS.getOrDefault(pos(), Set.of());
            fresh = new HashSet<>();
            for (Note note : improvements) if (!seen.contains(note.problem())) fresh.add(note.problem());
        }
        List<Note> shuffled = new ArrayList<>(talk);
        Collections.shuffle(shuffled, new Random(openSeed));
        List<Note> notes = new ArrayList<>(improvements);
        notes.addAll(shuffled);
        if (stack.isEmpty() && improvements.isEmpty() && !notes.isEmpty()) noteIndex = new Random(openSeed).nextInt(notes.size());
        stack = notes;
    }

    protected List<Note> stack() {
        return stack;
    }

    protected Set<String> fresh() {
        return fresh == null ? Set.of() : fresh;
    }

    /** Thumps the status stamp down when the building's status differs from the last one seen. */
    /** Eases the board's frost from what the player last saw here; see {@link FrostFade}. */
    @javax.annotation.Nullable
    private FrostFade frostFade;

    /** Records the board's own frost level, the header's; call it with {@link #trackStatus} on each rebuild. */
    protected void trackFrost(int level) {
        if (frostFade == null) frostFade = new FrostFade(pos());
        frostFade.track(level);
    }

    protected void trackStatus(Status status) {
        Status last = SEEN_STATUS.put(pos(), status);
        if (last != null && last != status) {
            stampAt = System.currentTimeMillis();
            CInputHelper.playUISound(SoundEvents.BOOK_PUT, 0.8f);
        }
    }

    /** The stamp's scale: larger for a moment after it changed, then 1. */
    protected float stampScale() {
        if (stampAt < 0) return 1;
        float p = (System.currentTimeMillis() - stampAt) / 160f;
        if (p >= 1) {
            stampAt = -1;
            return 1;
        }
        return 1 + 0.5f * (1 - p);
    }

    protected void markSeen(int index) {
        if (index < 0 || index >= stack.size()) return;
        Note note = stack.get(index);
        if (note.kind() != NoteKind.IMPROVE) return;
        SEEN_PROBLEMS.computeIfAbsent(pos(), p -> new HashSet<>()).add(note.problem());
        if (fresh != null) fresh.remove(note.problem());
    }

    /** The note stack of this screen, sized to its notes and remembering the shown note. */
    protected NoteStack noteStack(UILayer layer, int x, int y, int w, int maxH) {
        return BoardWidgets.notes(layer, stack, noteIndex, fresh(), index -> noteIndex = index, this::markSeen, x, y, w, maxH);
    }

    /**
     * 页头名字旁的「改名」小签：点开后名字变成输入框，回车把新名字交给服务端，留空恢复成类型加编号。放在高页头的页面上。
     * The rename tag beside the name on a tall header: it turns the name into a text box; Enter files the new name with
     * the server and a blank name restores type and number.
     */
    protected void renameTag(UILayer layer, Component currentName) {
        int w = contentWidth();
        if (!renaming) {
            BoardWidgets.renameTag(layer, w, currentName, Component.translatable("gui.frostedheart.board.rename.tooltip"), () -> {
                renaming = true;
                refreshElements();
            });
            return;
        }
        Object value = menu.getBlock().getBuilding().orElse(null);
        TeamTown town = menu.getBlock().getTown() instanceof TeamTown teamTown ? teamTown : null;
        String shown = currentName.getString();
        // The ghost shows what a blank name falls back to: the type and number.
        String fallback = value instanceof ITownBuilding building && town != null
                ? TownBuildingLabels.defaultName(town.getTownBuildings(), pos(), building).getString() : shown;
        boolean custom = value instanceof AbstractTownBuilding named && named.getCustomName() != null;
        BoardWidgets.renameBox(layer, w, shown, fallback, TownNamingModel.MAX_BUILDING_NAME_LENGTH, name -> {
            renaming = false;
            boolean unchanged = name.equals(shown) || (name.isEmpty() && !custom);
            if (!unchanged) FHNetwork.INSTANCE.sendToServer(new TownBuildingRenameRequestPacket(pos(), name));
            refreshElements();
        }, () -> {
            renaming = false;
            refreshElements();
        });
    }

    public void selectTab(Component title) {
        for (int i = 0; i < tabs.size(); i++)
            if (tabs.get(i).title().equals(title)) {
                tabSlide.leave(activeTab);
                activeTab = i;
                refreshElements();
                return;
            }
    }

    private long lastDataSerial = Long.MIN_VALUE;

    @Override
    public boolean onInit() {
        resizeToWindow();
        TeamTownData.addClientListener(this);
        return super.onInit();
    }

    private boolean resizeToWindow() {
        int guiW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int guiH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        // The layer holds the plaque column and the board, so centering keeps the plaques on screen.
        int w = Math.max(260, Math.min(MAX_W, guiW - 12 - PLAQUE_COLUMN)) + PLAQUE_COLUMN;
        int h = Math.max(200, Math.min(MAX_H, guiH - 12));
        if (w == getWidth() && h == getHeight()) return false;
        setSize(w, h);
        return true;
    }

    @Override
    public void addChildUIElements() {
        tabs.clear();
        boolean detailed = TownUiModeClient.detailed();
        modeSerial = TownUiModeClient.serial();
        addTabs(tabs, detailed);
        if (activeTab >= tabs.size()) activeTab = 0;
        beforeRebuild();
        content.setPosAndSize(PLAQUE_COLUMN + PAD, PAD, contentWidth(), contentHeight());
        add(content);
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            Tab tab = tabs.get(i);
            PlaqueTab plaque = new PlaqueTab(this, tab.icon(), tab.title(), tab.debug(), () -> activeTab == index, () -> {
                tabSlide.leave(activeTab);
                activeTab = index;
                refreshElements();
            }, () -> tabSlide.out(index, activeTab));
            plaque.setPos(4, 14 + i * 27);
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
        if (frostFade != null) frostFade.begin();
        else FrostFade.clear(null);
        NoticeBoardTheme.drawBoard(graphics, x + PLAQUE_COLUMN, y, w - PLAQUE_COLUMN, h);
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        boolean resized = resizeToWindow();
        boolean rebuild = resized || modeSerial != TownUiModeClient.serial();
        long serial = dataSerial();
        if (serial != lastDataSerial) {
            lastDataSerial = serial;
            rebuild = true;
        }
        // Debug sheets read their rows live, so a rebuild would only throw away the reader's scroll and selection.
        boolean liveTab = activeTab >= 0 && activeTab < tabs.size() && tabs.get(activeTab).debug();
        if (!liveTab) {
            if (dirty && ticks % 5 == 0) rebuild = true;
            if (ticks % 100 == 0) rebuild = true;
        } else if (modeSerial == TownUiModeClient.serial()) rebuild = resized;
        // A rename in progress keeps its box; only a window resize rebuilds under it.
        if (renaming && modeSerial == TownUiModeClient.serial()) rebuild = rebuild && resized;
        if (rebuild) {
            dirty = false;
            refreshElements();
        }
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
}
