/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
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
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
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

/** Drag-and-drop staffing queue with integer guaranteed-target sliders. */
public final class TownStaffingPanel extends UIElement {
    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_TOP = 32;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 6;
    private static final int HANDLE_RIGHT = 14;
    private static final int BAR_X = 128;
    private static final int BAR_Y_IN_ROW = 17;
    private static final int BAR_WIDTH = WIDTH - BAR_X - 8;
    private static final int BAR_HEIGHT = 6;
    private static final int SCROLLBAR_WIDTH = 3;

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
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        TeamTown town = townSource.get();
        syncDraft(town);
        Font font = Minecraft.getInstance().font;
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.staffing"),
                x + 4, y + 4, 0xFFFFAA00, true);
        renderSummary(graphics, font, x, y, town);
        renderRows(graphics, font, x, y, town);
        renderScrollbar(graphics, x, y);
        renderDropMarker(graphics, x, y);
    }

    private void syncDraft(@Nullable TeamTown town) {
        if (town == null || draggedBuilding != null || targetBuilding != null) return;
        TownStaffingPlan serverPlan = town.getStaffingPlan();
        if (serverPlan.equals(observedPlan)) return;
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
            graphics.drawString(font,
                    Component.translatable("gui.frostedheart.town_manager.no_data"),
                    x + 4, y + 17, 0xFFAAAAAA, false);
            return;
        }
        StaffingSummary summary = summary(town);
        Component text = Component.translatable(
                "gui.frostedheart.town_manager.staffing_summary",
                summary.availableWorkers(), summary.coveredTargets(),
                summary.totalTargets(), summary.unemployedWorkers());
        graphics.drawString(font,
                TownTextLayout.ellipsize(font, text.getString(), WIDTH - 8),
                x + 4, y + 17,
                summary.coveredTargets() < summary.totalTargets()
                        ? 0xFFFFAA00 : 0xFFAAAAAA,
                false);
    }

    private void renderRows(
            GuiGraphics graphics, Font font, int x, int y, @Nullable TeamTown town
    ) {
        if (town == null) return;
        Map<BlockPos, AbstractTownBuilding> buildings = town.getTownBuildings();
        List<Resident> residents = List.copyOf(town.getAllResidents());
        scroll = Mth.clamp(scroll, 0, maxScroll());
        for (int row = 0; row < VISIBLE_ROWS; row++) {
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

            int background = pos.equals(draggedBuilding)
                    ? 0xA0446688
                    : workable ? 0x90202020 : 0x90404040;
            graphics.fill(x + 2, rowY, x + WIDTH - 5, rowY + ROW_HEIGHT - 2, background);
            graphics.drawString(font, "≡", x + 4, rowY + 9,
                    workable ? 0xFFCCCCCC : 0xFF777777, false);
            String name = TownResidentsPanel.buildingName(
                    value.getClass().getSimpleName()).getString();
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, name, BAR_X - 19),
                    x + 15, rowY + 3, workable ? 0xFFFFFFFF : 0xFF999999, false);
            graphics.drawString(font,
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ(),
                    x + 15, rowY + 15, 0xFF888888, false);

            Component counts = Component.translatable(
                    "gui.frostedheart.town_manager.staffing_counts",
                    active, target, capacity);
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, counts.getString(), BAR_WIDTH),
                    x + BAR_X, rowY + 3,
                    !workable ? 0xFF888888
                            : active < effectiveTarget ? 0xFFFFAA00 : 0xFFFFFFFF,
                    false);
            renderCapacityBar(graphics, x + BAR_X, rowY + BAR_Y_IN_ROW,
                    active, assigned - active, target, capacity, workable);
        }
    }

    private static void renderCapacityBar(
            GuiGraphics graphics, int x, int y,
            int active, int absent, int target, int capacity, boolean workable
    ) {
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xFF181818);
        if (capacity <= 0 || !workable) {
            graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1,
                    0xFF555555);
            return;
        }
        int effectiveTarget = Math.min(target, capacity);
        int activeWidth = BAR_WIDTH * Math.min(active, capacity) / capacity;
        int targetWidth = BAR_WIDTH * effectiveTarget / capacity;
        int targetFilledWidth = Math.min(activeWidth, targetWidth);
        int targetColor = active >= effectiveTarget ? 0xFF55AA55 : 0xFFFFAA00;
        if (targetFilledWidth > 0) {
            graphics.fill(x, y, x + targetFilledWidth, y + BAR_HEIGHT, targetColor);
        }
        if (activeWidth > targetWidth) {
            graphics.fill(x + targetWidth, y, x + activeWidth, y + BAR_HEIGHT, 0xFF5599CC);
        }
        if (absent > 0) {
            int absentEnd = BAR_WIDTH * Math.min(capacity, active + absent) / capacity;
            graphics.fill(x + activeWidth, y + 1, x + absentEnd, y + BAR_HEIGHT - 1,
                    0xFFAA5555);
        }
        int marker = Mth.clamp(targetWidth, 0, BAR_WIDTH - 1);
        graphics.fill(x + marker, y - 1, x + marker + 1, y + BAR_HEIGHT + 1,
                target > capacity ? 0xFFFF5555 : 0xFFFFFFFF);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y) {
        int maximum = maxScroll();
        if (maximum <= 0) return;
        int trackX = x + WIDTH - SCROLLBAR_WIDTH - 1;
        int trackY = y + LIST_TOP;
        int trackHeight = VISIBLE_ROWS * ROW_HEIGHT - 2;
        int thumbHeight = Math.max(10,
                trackHeight * VISIBLE_ROWS / draftOrder.size());
        int thumbY = trackY + (trackHeight - thumbHeight) * scroll / maximum;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH,
                trackY + trackHeight, 0xFF222222);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH,
                thumbY + thumbHeight, 0xFFAAAAAA);
    }

    private void renderDropMarker(GuiGraphics graphics, int x, int y) {
        if (draggedBuilding == null) return;
        int visibleIndex = dragDropIndex - scroll;
        if (visibleIndex < 0 || visibleIndex > VISIBLE_ROWS) return;
        int markerY = y + LIST_TOP + visibleIndex * ROW_HEIGHT;
        graphics.fill(x + 2, markerY - 1, x + WIDTH - 5, markerY + 1, 0xFFFFFFFF);
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
        if (getMouseX() >= BAR_X && getMouseX() <= BAR_X + BAR_WIDTH) {
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
        } else if (getMouseX() >= BAR_X && getMouseX() <= BAR_X + BAR_WIDTH) {
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
        if (getMouseY() > LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT - 7
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
                (getMouseX() - BAR_X) / (double) BAR_WIDTH, 0.0, 1.0);
        draftTargets.put(targetBuilding, (int) Math.round(fraction * capacity));
    }

    private int rowAtMouse() {
        if (getMouseY() < LIST_TOP
                || getMouseY() >= LIST_TOP + VISIBLE_ROWS * ROW_HEIGHT) return -1;
        return scroll + (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, draftOrder.size() - VISIBLE_ROWS);
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
            if (eligible) {
                available++;
                if (resident.getWorkPos() == null) unemployed++;
            }
        }
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

    private static void drawPanel(
            GuiGraphics graphics, int x, int y, int width, int height
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private record StaffingSummary(
            int availableWorkers,
            int coveredTargets,
            int totalTargets,
            int unemployedWorkers
    ) {
    }
}
