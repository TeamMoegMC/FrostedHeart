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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.network.TownStaffingEditRequestPacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.client.board.BoardQueue;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import com.teammoeg.frostedheart.content.town.ui.TownBuildingLabels;
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
 * 岗位调度：一列可以拖动排序的工单纸条，每张带一道纸槽，黄铜图钉是保障人数的滑块。放在布告板上，尺寸随板子。
 * <p>
 * The staffing queue: paper work orders dragged into priority order, each with a paper track whose brass pin sets
 * the guaranteed workers. Lives on the notice board and takes the board's size.
 */
public final class TownStaffingPanel extends UIElement {
    private static final int LIST_TOP = 30;
    private static final int ROW_HEIGHT = 30;
    private static final int HANDLE_RIGHT = 16;
    private static final int BAR_Y_IN_ROW = 18;
    private static final int BAR_HEIGHT = 6;
    private static final int SCROLLBAR_WIDTH = 8;

    private final Supplier<TeamTown> townSource;
    private TownStaffingPlan observedPlan = TownStaffingPlan.EMPTY;
    private final List<BlockPos> draftOrder = new ArrayList<>();
    private final Map<BlockPos, Integer> draftTargets = new HashMap<>();
    private int scroll;

    @Nullable
    private BlockPos draggedBuilding;
    private int dragDropIndex;
    @Nullable
    private BlockPos targetBuilding;
    private int targetBeforeDrag;

    public TownStaffingPanel(
            UIElement parent, int x, int y, Supplier<TeamTown> townSource
    ) {
        this(parent, x, y, TownManagerScreen.CONTENT_WIDTH, TownManagerScreen.CONTENT_HEIGHT, townSource);
    }

    public TownStaffingPanel(UIElement parent, int x, int y, int width, int height, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(width, height);
    }

    private int visibleRows() {
        return Math.max(1, (getHeight() - LIST_TOP) / ROW_HEIGHT);
    }

    private int barX() {
        return Math.max(130, getWidth() * 48 / 100);
    }

    private int barWidth() {
        return getWidth() - barX() - SCROLLBAR_WIDTH - 14;
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        TeamTown town = townSource.get();
        syncDraft(town);
        Font font = Minecraft.getInstance().font;
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                Component.translatable("gui.frostedheart.town_manager.staffing").copy().withStyle(net.minecraft.ChatFormatting.BOLD),
                x + 4, y + 2, NoticeBoardTheme.ON_WOOD);
        renderSummary(graphics, font, x, y, town);
        renderRows(graphics, font, x, y, town);
        renderScrollbar(graphics, x, y);
        renderDropMarker(graphics, x, y);
        // Below the last strip, where the board would stand empty, a note says how the page works.
        int listBottom = y + LIST_TOP + Math.min(draftOrder.size(), visibleRows()) * ROW_HEIGHT + 8;
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.helpNote(graphics,
                net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.staffing.help.title"),
                java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.staffing.help.order"),
                        net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.staffing.help.pin"),
                        net.minecraft.network.chat.Component.translatable("gui.frostedheart.board.town.staffing.help.colors")),
                x + 8, listBottom, Math.min(getWidth() - 24, 300), y + getHeight() - listBottom - 4);
    }

    private void syncDraft(@Nullable TeamTown town) {
        if (town == null || draggedBuilding != null || targetBuilding != null) return;
        TownStaffingPlan serverPlan = town.getStaffingPlan();
        // Equal contents can be a new authoritative reply rejecting the local draft.
        if (serverPlan == observedPlan) return;
        observedPlan = serverPlan;
        draftOrder.clear();
        draftTargets.clear();
        for (TownStaffingPlan.Entry entry : serverPlan.entries()) {
            draftOrder.add(entry.building());
            draftTargets.put(entry.building(), entry.targetWorkers());
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private void renderSummary(
            GuiGraphics graphics, Font font, int x, int y, @Nullable TeamTown town
    ) {
        if (town == null) {
            com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                    Component.translatable("gui.frostedheart.town_manager.no_data"),
                    x + 4, y + 14, NoticeBoardTheme.ON_WOOD);
            return;
        }
        StaffingSummary summary = summary(town);
        Component text = Component.translatable(
                "gui.frostedheart.town_manager.staffing_summary",
                summary.availableWorkers(), summary.coveredTargets(),
                summary.totalTargets(), summary.unemployedWorkers());
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                TownTextLayout.ellipsize(font, text.getString(), getWidth() - 8),
                x + 4, y + 14,
                summary.coveredTargets() < summary.totalTargets()
                        ? 0xFFFAC775 : NoticeBoardTheme.ON_WOOD);
    }

    private void renderRows(
            GuiGraphics graphics, Font font, int x, int y, @Nullable TeamTown town
    ) {
        if (town == null) return;
        Map<BlockPos, AbstractTownBuilding> buildings = town.getTownBuildings();
        List<Resident> residents = List.copyOf(town.getAllResidents());
        scroll = Mth.clamp(scroll, 0, maxScroll());
        int barX = barX(), barWidth = barWidth(), stripRight = getWidth() - SCROLLBAR_WIDTH - 6;
        for (int row = 0; row < visibleRows(); row++) {
            int index = scroll + row;
            if (index >= draftOrder.size()) break;
            BlockPos pos = draftOrder.get(index);
            AbstractTownBuilding value = buildings.get(pos);
            if (!(value instanceof ITownResidentWorkBuilding building)) continue;
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            boolean workable = value.isBuildingWorkable();
            int capacity = Math.max(0, building.getMaxResidents());
            int target = Math.max(0, draftTargets.getOrDefault(pos, 0));
            int effectiveTarget = workable ? Math.min(target, capacity) : 0;
            int assigned = 0;
            int active = 0;
            for (Resident resident : residents) {
                if (!pos.equals(resident.getWorkPos())) continue;
                assigned++;
                if (workable && building.canResidentWork(resident)) active++;
            }

            boolean lifted = pos.equals(draggedBuilding);
            BoardQueue.strip(graphics, x + 2, rowY, stripRight - 2, ROW_HEIGHT - 4, lifted, !workable);
            BoardQueue.handle(graphics, x + 7, rowY + 9, !workable);
            Component name = TownBuildingLabels.name(buildings, pos, value);
            // A stopped building keeps its name in ink; the red line below says it is stopped.
            graphics.drawString(font, TownTextLayout.ellipsize(font, name.getString(), barX - 26), x + 19, rowY + 4,
                    NoticeBoardTheme.INK, false);
            Component where = workable ? TownBuildingLabels.subtitle(value, town.getTowerPos(), pos)
                    : Component.translatable("gui.frostedheart.board.town.staffing.closed");
            graphics.drawString(font, TownTextLayout.ellipsize(font, where.getString(), barX - 26), x + 19, rowY + 15,
                    workable ? NoticeBoardTheme.MUTED : NoticeBoardTheme.RED, false);

            Component counts = Component.translatable("gui.frostedheart.board.town.staffing.counts", active, target, capacity);
            graphics.drawString(font, TownTextLayout.ellipsize(font, counts.getString(), barWidth), x + barX, rowY + 4,
                    !workable ? NoticeBoardTheme.MUTED : active < effectiveTarget ? NoticeBoardTheme.AMBER : NoticeBoardTheme.INK, false);
            renderCapacityBar(graphics, x + barX, rowY + BAR_Y_IN_ROW, barWidth,
                    active, assigned - active, target, capacity, workable);
        }
    }

    private static void renderCapacityBar(
            GuiGraphics graphics, int x, int y, int barWidth,
            int active, int absent, int target, int capacity, boolean workable
    ) {
        BoardQueue.track(graphics, x, y, barWidth, BAR_HEIGHT);
        if (capacity <= 0) return;
        if (!workable) {
            // A closed building keeps its setting for when it opens again; show where the pin stands.
            BoardQueue.knob(graphics, x, y, Mth.clamp(barWidth * Math.min(target, capacity) / capacity, 0, barWidth - 1), BAR_HEIGHT, false);
            return;
        }
        int effectiveTarget = Math.min(target, capacity);
        int activeWidth = barWidth * Math.min(active, capacity) / capacity;
        int targetWidth = barWidth * effectiveTarget / capacity;
        int targetFilledWidth = Math.min(activeWidth, targetWidth);
        BoardQueue.fill(graphics, x, y, 0, targetFilledWidth, BAR_HEIGHT, active >= effectiveTarget ? BoardQueue.GOOD : BoardQueue.WARN);
        if (activeWidth > targetWidth) BoardQueue.fill(graphics, x, y, targetWidth, activeWidth, BAR_HEIGHT, BoardQueue.EXTRA);
        if (absent > 0) {
            int absentEnd = barWidth * Math.min(capacity, active + absent) / capacity;
            BoardQueue.fill(graphics, x, y, activeWidth, absentEnd, BAR_HEIGHT, BoardQueue.BAD);
        }
        BoardQueue.knob(graphics, x, y, Mth.clamp(targetWidth, 0, barWidth - 1), BAR_HEIGHT, target > capacity);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y) {
        int maximum = maxScroll();
        if (maximum <= 0) return;
        int trackX = x + getWidth() - SCROLLBAR_WIDTH;
        int trackY = y + LIST_TOP;
        int trackHeight = visibleRows() * ROW_HEIGHT - 4;
        int thumbHeight = Math.max(12, trackHeight * visibleRows() / draftOrder.size());
        int thumbY = trackY + (trackHeight - thumbHeight) * scroll / maximum;
        NoticeBoardTheme.INSTANCE.drawSliderBackground(graphics, trackX, trackY, SCROLLBAR_WIDTH, trackHeight, false);
        NoticeBoardTheme.INSTANCE.drawSliderBar(graphics, trackX + 1, thumbY, SCROLLBAR_WIDTH - 2, thumbHeight, false);
    }

    private void renderDropMarker(GuiGraphics graphics, int x, int y) {
        if (draggedBuilding == null) return;
        int visibleIndex = dragDropIndex - scroll;
        if (visibleIndex < 0 || visibleIndex > visibleRows()) return;
        int markerY = y + LIST_TOP + visibleIndex * ROW_HEIGHT - 2;
        BoardQueue.dropLine(graphics, x + 4, markerY, getWidth() - SCROLLBAR_WIDTH - 14);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        int index = rowAtMouse();
        if (index < 0 || index >= draftOrder.size()) return false;
        BlockPos pos = draftOrder.get(index);
        if (getMouseX() < HANDLE_RIGHT) {
            draggedBuilding = pos;
            dragDropIndex = index;
            return true;
        }
        if (getMouseX() >= barX() && getMouseX() <= barX() + barWidth()) {
            AbstractTownBuilding value = building(pos);
            if (!(value instanceof ITownResidentWorkBuilding building)
                    || building.getMaxResidents() <= 0) return false;
            targetBuilding = pos;
            targetBeforeDrag = draftTargets.getOrDefault(pos, 0);
            updateTargetFromMouse(building.getMaxResidents());
            return true;
        }
        return false;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;
        int index = rowAtMouse();
        if (index < 0 || index >= draftOrder.size()) return;
        BlockPos pos = draftOrder.get(index);
        AbstractTownBuilding value = building(pos);
        if (getMouseX() < HANDLE_RIGHT) {
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.staffing_tooltip_order"));
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.staffing_tooltip_next_day"));
        } else if (getMouseX() >= barX() && getMouseX() <= barX() + barWidth()) {
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.staffing_tooltip_target"));
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.staffing_tooltip_next_day"));
        }
        if (value != null && !value.isBuildingWorkable()) {
            tooltip.accept(Component.translatable(
                    "gui.frostedheart.town_manager.staffing_tooltip_unworkable"));
        }
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (button != MouseButton.LEFT) return false;
        if (targetBuilding != null) {
            AbstractTownBuilding value = building(targetBuilding);
            if (value instanceof ITownResidentWorkBuilding building) {
                updateTargetFromMouse(building.getMaxResidents());
            }
            return true;
        }
        if (draggedBuilding == null) return false;
        if (getMouseY() < LIST_TOP + 7 && scroll > 0) scroll--;
        if (getMouseY() > LIST_TOP + visibleRows() * ROW_HEIGHT - 7
                && scroll < maxScroll()) scroll++;
        double relative = getMouseY() - LIST_TOP + ROW_HEIGHT / 2.0;
        dragDropIndex = Mth.clamp(
                scroll + (int) Math.floor(relative / ROW_HEIGHT),
                0, draftOrder.size());
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        if (button == MouseButton.LEFT && targetBuilding != null) {
            int target = draftTargets.getOrDefault(targetBuilding, 0);
            if (target != targetBeforeDrag) {
                FHNetwork.INSTANCE.sendToServer(
                        TownStaffingEditRequestPacket.setTarget(targetBuilding, target));
            }
            targetBuilding = null;
        }
        if (button == MouseButton.LEFT && draggedBuilding != null) {
            int oldIndex = draftOrder.indexOf(draggedBuilding);
            BlockPos moved = draggedBuilding;
            if (oldIndex >= 0) {
                draftOrder.remove(oldIndex);
                int insertion = dragDropIndex;
                if (insertion > oldIndex) insertion--;
                insertion = Mth.clamp(insertion, 0, draftOrder.size());
                draftOrder.add(insertion, moved);
                if (insertion != oldIndex) {
                    Optional<BlockPos> before = insertion + 1 < draftOrder.size()
                            ? Optional.of(draftOrder.get(insertion + 1))
                            : Optional.empty();
                    FHNetwork.INSTANCE.sendToServer(
                            TownStaffingEditRequestPacket.move(moved, before));
                }
            }
            draggedBuilding = null;
        }
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if (!isMouseOver() || maxScroll() == 0) return false;
        scroll = Mth.clamp(scroll - (int) Math.signum(amount), 0, maxScroll());
        return true;
    }

    private void updateTargetFromMouse(int capacity) {
        if (targetBuilding == null || capacity <= 0) return;
        double fraction = Mth.clamp(
                (getMouseX() - barX()) / (double) barWidth(), 0.0, 1.0);
        draftTargets.put(targetBuilding, (int) Math.round(fraction * capacity));
    }

    private int rowAtMouse() {
        if (getMouseY() < LIST_TOP
                || getMouseY() >= LIST_TOP + visibleRows() * ROW_HEIGHT) return -1;
        return scroll + (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, draftOrder.size() - visibleRows());
    }

    @Nullable
    private AbstractTownBuilding building(BlockPos pos) {
        TeamTown town = townSource.get();
        return town == null ? null : town.getTownBuildings().get(pos);
    }

    private static StaffingSummary summary(TeamTown town) {
        List<AbstractTownBuilding> workBuildings = town.getTownBuildings().values().stream()
                .filter(value -> value instanceof ITownResidentWorkBuilding)
                .toList();
        int available = 0;
        int unemployed = 0;
        for (Resident resident : town.getAllResidents()) {
            boolean eligible = workBuildings.stream().anyMatch(value ->
                    value.isBuildingWorkable()
                            && ((ITownResidentWorkBuilding) value).canResidentWork(resident));
            if (eligible) available++;
        }
        // 空闲 counts the same people as the residents page: fit, old enough and without a job.
        unemployed = com.teammoeg.frostedheart.content.town.ui.view.BoardPresenters.headcount(List.copyOf(town.getAllResidents()), town.getTownBuildings()).free();
        int targets = 0;
        int covered = 0;
        for (TownStaffingPlan.Entry entry : town.getStaffingPlan().entries()) {
            AbstractTownBuilding value = town.getTownBuildings().get(entry.building());
            if (!(value instanceof ITownResidentWorkBuilding building)
                    || !value.isBuildingWorkable()) continue;
            int effective = Math.min(entry.targetWorkers(), building.getMaxResidents());
            int active = (int) town.getAllResidents().stream()
                    .filter(resident -> entry.building().equals(resident.getWorkPos()))
                    .filter(building::canResidentWork)
                    .count();
            targets += effective;
            covered += Math.min(active, effective);
        }
        return new StaffingSummary(available, covered, targets, unemployed);
    }


    private record StaffingSummary(
            int availableWorkers,
            int coveredTargets,
            int totalTargets,
            int unemployedWorkers
    ) {
    }
}
