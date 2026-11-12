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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.network.TownHousingEditRequestPacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.client.board.BoardDraw;
import com.teammoeg.frostedheart.content.town.client.board.BoardQueue;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
import com.teammoeg.frostedheart.content.town.ui.TownFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 住房：一列可以拖动排序的住宅纸条（越靠前越先住人），每张带一道纸槽，黄铜图钉是保障口粮人数的滑块。尺寸随布告板。
 * <p>
 * Housing: house strips dragged into priority order (earlier houses fill first), each with a paper track whose brass
 * pin sets the residents guaranteed a ration. Takes the board's size.
 */
public final class TownHousingPanel extends UIElement {
    private static final int LIST_TOP = 30;
    private static final int ROW_HEIGHT = 30;
    private static final int HANDLE_RIGHT = 16;
    private static final int BAR_Y = 18;
    private static final int SCROLLBAR_WIDTH = 8;
    private final Supplier<TeamTown> townSource;
    private TownHousingPlan observed = TownHousingPlan.EMPTY;
    private final List<BlockPos> draftOrder = new ArrayList<>();
    private final Map<BlockPos, Integer> draftGuarantees = new HashMap<>();
    private int scroll;
    @Nullable private BlockPos dragged;
    private int dropIndex;
    @Nullable private BlockPos editingTarget;
    private int targetBeforeDrag;

    public TownHousingPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        this(parent, x, y, TownManagerScreen.CONTENT_WIDTH, TownManagerScreen.CONTENT_HEIGHT, townSource);
    }

    public TownHousingPanel(UIElement parent, int x, int y, int width, int height, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(width, height);
    }

    private int visibleRows() {
        return Math.max(1, (getHeight() - LIST_TOP) / ROW_HEIGHT);
    }

    private int barX() {
        return Math.max(140, getWidth() * 50 / 100);
    }

    private int barWidth() {
        return getWidth() - barX() - SCROLLBAR_WIDTH - 14;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderingHint hint) {
        TeamTown town = townSource.get();
        sync(town);
        Font font = Minecraft.getInstance().font;
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                Component.translatable("gui.frostedheart.town_manager.housing_care").copy().withStyle(net.minecraft.ChatFormatting.BOLD),
                x + 4, y + 2, NoticeBoardTheme.ON_WOOD);
        if (town == null) return;
        int housed = (int) town.getAllResidents().stream()
                .filter(resident -> resident.getHousePos() != null).count();
        int guarantees = draftGuarantees.values().stream().mapToInt(Integer::intValue).sum();
        Component summary = Component.translatable(
                "gui.frostedheart.town_manager.housing_summary",
                housed, town.getAllResidents().size(), guarantees);
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                TownTextLayout.ellipsize(font, summary.getString(), width - 8),
                x + 4, y + 14, NoticeBoardTheme.ON_WOOD);
        renderRows(graphics, font, x, y, town);
        renderScrollbar(graphics, x, y);
        renderDropMarker(graphics, x, y);
        // Below the last strip, where the board would stand empty, a note says how the page works.
        int listBottom = y + LIST_TOP + Math.min(draftOrder.size(), visibleRows()) * ROW_HEIGHT + 8;
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.helpNote(graphics,
                net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.housing.help.title"),
                java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.housing.help.order"),
                        net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.housing.help.pin"),
                        net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.housing.help.colors")),
                x + 8, listBottom, Math.min(getWidth() - 24, 300), y + getHeight() - listBottom - 4);
    }

    private void sync(@Nullable TeamTown town) {
        if (town == null || dragged != null || editingTarget != null) return;
        TownHousingPlan plan = town.getHousingPlan();
        // A fresh reply must reset the draft even when its guarantees are unchanged.
        if (plan == observed) return;
        observed = plan;
        draftOrder.clear();
        draftGuarantees.clear();
        for (TownHousingPlan.Entry entry : plan.entries()) {
            draftOrder.add(entry.building());
            draftGuarantees.put(entry.building(), entry.guaranteedResidents());
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private void renderRows(GuiGraphics graphics, Font font, int x, int y, TeamTown town) {
        Map<BlockPos, AbstractTownBuilding> buildings = town.getTownBuildings();
        int barX = barX(), barWidth = barWidth(), stripRight = getWidth() - SCROLLBAR_WIDTH - 6;
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= draftOrder.size()) break;
            BlockPos pos = draftOrder.get(index);
            AbstractTownBuilding value = buildings.get(pos);
            if (!(value instanceof HouseBuilding house)) continue;
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            boolean workable = house.isBuildingWorkable();
            int capacity = Math.max(0, house.getMaxResidents());
            int guarantee = Math.max(0, draftGuarantees.getOrDefault(pos, 0));
            int occupancy = (int) town.getAllResidents().stream()
                    .filter(resident -> pos.equals(resident.getHousePos())).count();
            BoardQueue.strip(graphics, x + 2, rowY, stripRight - 2, ROW_HEIGHT - 4, pos.equals(dragged), !workable);
            BoardQueue.handle(graphics, x + 7, rowY + 9, !workable);
            Component name = TownBuildingLabels.name(buildings, pos, house);
            int nameW = font.width(name);
            graphics.drawString(font, TownTextLayout.ellipsize(font, name.getString(), barX - 70), x + 19, rowY + 4,
                    NoticeBoardTheme.INK, false);
            // Comfort as stars beside the name, the same rating the house's own board shows.
            if (19 + nameW + 46 < barX) BoardDraw.stars(graphics, x + 19 + nameW + 5, rowY + 4, TownFormat.stars(house.getComfortRating()));
            Component where = workable ? TownBuildingLabels.subtitle(house, town.getTowerPos(), pos)
                    : Component.translatable("gui.frostedheart.board.town.housing.unfit");
            graphics.drawString(font, TownTextLayout.ellipsize(font, where.getString(), barX - 26), x + 19, rowY + 15,
                    workable ? NoticeBoardTheme.MUTED : NoticeBoardTheme.RED, false);
            Component detail = Component.translatable("gui.frostedheart.board.town.housing.counts", occupancy, guarantee, capacity);
            graphics.drawString(font, TownTextLayout.ellipsize(font, detail.getString(), barWidth), x + barX, rowY + 4,
                    workable ? NoticeBoardTheme.INK : NoticeBoardTheme.MUTED, false);
            renderBar(graphics, x + barX, rowY + BAR_Y, barWidth, occupancy, guarantee, capacity, workable);
        }
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, int barWidth,
                                  int occupancy, int guarantee, int capacity,
                                  boolean workable) {
        BoardQueue.track(graphics, x, y, barWidth, 6);
        if (capacity <= 0) return;
        int occupiedWidth = barWidth * Math.min(occupancy, capacity) / capacity;
        int guaranteeWidth = barWidth * Math.min(guarantee, capacity) / capacity;
        // An unfit house keeps its people and its setting, so both stay on the strip, greyed.
        BoardQueue.fill(graphics, x, y, 0, occupiedWidth, 6, workable ? BoardQueue.EXTRA : BoardQueue.OFF);
        BoardQueue.knob(graphics, x, y, Mth.clamp(guaranteeWidth, 0, barWidth - 1), 6, false);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y) {
        int maximum = maxScroll();
        if (maximum <= 0) return;
        int trackX = x + getWidth() - SCROLLBAR_WIDTH, trackY = y + LIST_TOP, trackHeight = visibleRows() * ROW_HEIGHT - 4;
        int thumbHeight = Math.max(12, trackHeight * visibleRows() / draftOrder.size());
        int thumbY = trackY + (trackHeight - thumbHeight) * scroll / maximum;
        NoticeBoardTheme.INSTANCE.drawSliderBackground(graphics, trackX, trackY, SCROLLBAR_WIDTH, trackHeight, false);
        NoticeBoardTheme.INSTANCE.drawSliderBar(graphics, trackX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (button != MouseButton.LEFT || !isMouseOver()) return false;
        int index = rowAtMouse();
        if (index < 0 || index >= draftOrder.size()) return false;
        BlockPos pos = draftOrder.get(index);
        if (getMouseX() < HANDLE_RIGHT) {
            dragged = pos;
            dropIndex = index;
            return true;
        }
        if (getMouseX() >= barX() && getMouseX() <= barX() + barWidth()) {
            AbstractTownBuilding value = building(pos);
            if (!(value instanceof HouseBuilding house) || house.getMaxResidents() <= 0) return false;
            editingTarget = pos;
            targetBeforeDrag = draftGuarantees.getOrDefault(pos, 0);
            updateTarget(house.getMaxResidents());
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (button != MouseButton.LEFT) return false;
        if (editingTarget != null) {
            AbstractTownBuilding value = building(editingTarget);
            if (value instanceof HouseBuilding house) updateTarget(house.getMaxResidents());
            return true;
        }
        if (dragged == null) return false;
        dropIndex = Mth.clamp(scroll + (int) Math.floor(
                (getMouseY() - LIST_TOP + ROW_HEIGHT / 2.0) / ROW_HEIGHT),
                0, draftOrder.size());
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        if (button == MouseButton.LEFT && editingTarget != null) {
            int target = draftGuarantees.getOrDefault(editingTarget, 0);
            if (target != targetBeforeDrag) FHNetwork.INSTANCE.sendToServer(
                    TownHousingEditRequestPacket.setGuarantee(editingTarget, target));
            editingTarget = null;
        }
        if (button == MouseButton.LEFT && dragged != null) {
            int old = draftOrder.indexOf(dragged);
            BlockPos moved = dragged;
            if (old >= 0) {
                draftOrder.remove(old);
                int insertion = dropIndex > old ? dropIndex - 1 : dropIndex;
                insertion = Mth.clamp(insertion, 0, draftOrder.size());
                draftOrder.add(insertion, moved);
                if (insertion != old) {
                    Optional<BlockPos> before = insertion + 1 < draftOrder.size()
                            ? Optional.of(draftOrder.get(insertion + 1)) : Optional.empty();
                    FHNetwork.INSTANCE.sendToServer(
                            TownHousingEditRequestPacket.move(moved, before));
                }
            }
            dragged = null;
        }
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (!isMouseOver() || maxScroll() <= 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;
        int index = rowAtMouse();
        if (index < 0 || index >= draftOrder.size()) return;
        // Help where the hand is: the handle explains the order, the track the guarantee.
        if (getMouseX() < HANDLE_RIGHT)
            tooltip.accept(Component.translatable("gui.frostedheart.town_manager.housing_priority_help"));
        else if (getMouseX() >= barX() && getMouseX() <= barX() + barWidth())
            tooltip.accept(Component.translatable("gui.frostedheart.town_manager.housing_guarantee_help"));
        else return;
        tooltip.accept(Component.translatable("gui.frostedheart.town_manager.staffing_tooltip_next_day"));
    }

    private void updateTarget(int capacity) {
        double fraction = Mth.clamp((getMouseX() - barX()) / (double) barWidth(), 0.0, 1.0);
        draftGuarantees.put(editingTarget, (int) Math.round(fraction * capacity));
    }

    private int rowAtMouse() {
        if (getMouseY() < LIST_TOP || getMouseY() >= LIST_TOP + visibleRows() * ROW_HEIGHT) return -1;
        return scroll + (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
    }

    private int maxScroll() { return Math.max(0, draftOrder.size() - visibleRows()); }

    @Nullable
    private AbstractTownBuilding building(BlockPos pos) {
        TeamTown town = townSource.get();
        return town == null ? null : town.getTownBuildings().get(pos);
    }

    private void renderDropMarker(GuiGraphics graphics, int x, int y) {
        if (dragged == null) return;
        int visible = dropIndex - scroll;
        if (visible < 0 || visible > visibleRows()) return;
        BoardQueue.dropLine(graphics, x + 4, y + LIST_TOP + visible * ROW_HEIGHT - 2, getWidth() - SCROLLBAR_WIDTH - 14);
    }
}
