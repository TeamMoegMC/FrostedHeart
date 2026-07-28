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
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 村民页签的内容面板。左侧为可滚动的居民名单，点击选择后
 * 右侧显示该居民的属性、住房/工作分配与各项熟练度。
 * 布局与交互参照 {@link com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel}。
 * <p>
 * Content panel of the residents tab. A scrollable resident list on the left;
 * clicking a resident shows their attributes, housing/work assignment and work
 * proficiencies on the right. Layout and interaction follow TownWorkforcePanel.
 */
public class TownResidentsPanel extends UIElement {

    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_WIDTH = 62;
    private static final int LIST_TOP = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = (HEIGHT - LIST_TOP - 4) / ROW_HEIGHT;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int DETAIL_TOP = 6;
    private static final int DETAIL_VISIBLE = (HEIGHT - DETAIL_TOP - 4) / DETAIL_LINE_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int DETAIL_TEXT_WIDTH = WIDTH - LIST_WIDTH - SCROLLBAR_WIDTH - 13;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<TeamTown> townSource;

    @Nullable
    private UUID selectedResident;
    private int listScroll;
    private int detailScroll;
    private boolean draggingDetailBar;

    public TownResidentsPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        Font font = Minecraft.getInstance().font;
        List<Resident> residents = residents();
        Resident selected = normalizeSelection(residents);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1, 0xFF777777);
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.residents"),
                x + 4, y + 6, 0xFFFFAA00, true);
        renderList(graphics, font, x, y, residents);
        renderDetails(graphics, font, x, y, detailLines(selected));
    }

    private void renderList(GuiGraphics graphics, Font font, int x, int y, List<Resident> residents) {
        int maxScroll = Math.max(0, residents.size() - VISIBLE_ROWS);
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = listScroll + row;
            if (index >= residents.size()) break;
            Resident resident = residents.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            if (resident.getUUID().equals(selectedResident)) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0xA0446688);
            } else if (getMouseX() >= 2 && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0x60444444);
            }
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font, resident.toString(), LIST_WIDTH - 9),
                    x + 5, rowY + 4, 0xFFFFFFFF, true);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * VISIBLE_ROWS / residents.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * listScroll / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackY, x + LIST_WIDTH - 1, trackY + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY, x + LIST_WIDTH - 1, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderDetails(GuiGraphics graphics, Font font, int x, int y, List<Line> lines) {
        List<VisualLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - DETAIL_VISIBLE);
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);
        int textX = x + LIST_WIDTH + 5;
        graphics.enableScissor(x + LIST_WIDTH + 1, y + 1, x + WIDTH - SCROLLBAR_WIDTH - 3, y + HEIGHT - 1);
        for (int row = 0; row < DETAIL_VISIBLE; row++) {
            int index = detailScroll + row;
            if (index >= visualLines.size()) break;
            VisualLine line = visualLines.get(index);
            graphics.drawString(font, line.text(),
                    textX, y + DETAIL_TOP + row * DETAIL_LINE_HEIGHT, line.color(), true);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = x + WIDTH - SCROLLBAR_WIDTH - 2;
            int trackY = y + 2;
            int trackHeight = HEIGHT - 4;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / visualLines.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * detailScroll / maxScroll;
            graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
            graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                    draggingDetailBar ? 0xFFD0D0D0 : 0xFF909090);
        }
    }

    private List<Line> detailLines(@Nullable Resident resident) {
        if (resident == null) {
            return List.of(new Line(
                    Component.translatable("gui.frostedheart.town_manager.no_residents"), 0xFFAAAAAA));
        }
        TeamTown town = townSource.get();
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.literal(resident.toString()), 0xFFFFAA00));
        lines.add(stat("gui.frostedheart.town.health", resident.getHealth()));
        lines.add(stat("gui.frostedheart.town.mental", resident.getMental()));
        lines.add(stat("gui.frostedheart.town.strength", resident.getStrength()));
        lines.add(stat("gui.frostedheart.town.intelligence", resident.getIntelligence()));
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.education")
                .append(Component.literal(": " + resident.getEducationLevel())), 0xFFFFFFFF));
        lines.add(new Line(Component.empty(), 0xFFFFFFFF));
        lines.add(assignment("gui.frostedheart.town_manager.house", resident.getHousePos(), town));
        lines.add(assignment("gui.frostedheart.town_manager.work", resident.getWorkPos(), town));
        if (!resident.getWorkProficiency().isEmpty()) {
            lines.add(new Line(Component.empty(), 0xFFFFFFFF));
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.proficiency"), 0xFFFFFF55));
            for (Map.Entry<String, Double> entry : resident.getWorkProficiency().entrySet()) {
                lines.add(new Line(Component.literal("• ")
                        .append(buildingName(entry.getKey()))
                        .append(Component.literal(": " + Math.round(entry.getValue()))), 0xFFFFFFFF));
            }
        }
        return lines;
    }

    private Line assignment(String labelKey, @Nullable BlockPos pos, @Nullable TeamTown town) {
        Component label = Component.translatable(labelKey).append(Component.literal(": "));
        if (pos == null || town == null) {
            return new Line(label.copy().append(
                    Component.translatable("gui.frostedheart.town_manager.none")), 0xFFFF5555);
        }
        AbstractTownBuilding building = town.getTownBuilding(pos).orElse(null);
        Component name = building == null
                ? Component.translatable("gui.frostedheart.town_manager.none")
                : buildingName(building.getClass().getSimpleName());
        return new Line(label.copy().append(name), 0xFF55FF55);
    }

    /**
     * 建筑类型的显示名。优先使用本地化键，缺失时回退到类名。
     * <p>
     * Display name of a building type. Prefers localization, falls back to the
     * class simple name.
     */
    static Component buildingName(String simpleClassName) {
        return Component.translatableWithFallback(
                "gui.frostedheart.town_manager.building." + simpleClassName, simpleClassName);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (getMouseX() >= WIDTH - SCROLLBAR_WIDTH - 2 && detailScrollable()) {
            draggingDetailBar = true;
            updateDetailScrollFromMouse();
            return true;
        }
        if (getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_ROWS) return false;
        List<Resident> residents = residents();
        int index = listScroll + row;
        if (index >= residents.size()) return false;
        selectedResident = residents.get(index).getUUID();
        detailScroll = 0;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        if (getMouseX() < LIST_WIDTH) {
            int max = Math.max(0, residents().size() - VISIBLE_ROWS);
            if (max == 0) return false;
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scroll), 0, max);
            return true;
        }
        int max = Math.max(0, wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size() - DETAIL_VISIBLE);
        if (max == 0) return false;
        detailScroll = Mth.clamp(detailScroll - (int) Math.signum(scroll), 0, max);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!draggingDetailBar) return super.onMouseDragged(button, dragX, dragY);
        updateDetailScrollFromMouse();
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        draggingDetailBar = false;
        super.onMouseReleased(button);
    }

    private void updateDetailScrollFromMouse() {
        int lineCount = wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size();
        int max = Math.max(0, lineCount - DETAIL_VISIBLE);
        if (max == 0) {
            detailScroll = 0;
            return;
        }
        int trackHeight = HEIGHT - 4;
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / lineCount);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) return;
        double top = getMouseY() - 2 - thumbHeight / 2.0;
        detailScroll = (int) Math.round(Mth.clamp(top / travel, 0.0, 1.0) * max);
    }

    private boolean detailScrollable() {
        return wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(residents()))).size() > DETAIL_VISIBLE;
    }

    /**
     * 维持选中态：居民消失时回退到列表首位，列表为空时清空选择。
     * <p>
     * Keeps the selection valid: falls back to the first resident when the
     * selected one disappears, clears selection when the list is empty.
     */
    @Nullable
    private Resident normalizeSelection(List<Resident> residents) {
        if (residents.isEmpty()) {
            selectedResident = null;
            listScroll = 0;
            detailScroll = 0;
            return null;
        }
        for (Resident resident : residents) {
            if (resident.getUUID().equals(selectedResident)) return resident;
        }
        selectedResident = residents.get(0).getUUID();
        detailScroll = 0;
        return residents.get(0);
    }

    private List<Resident> residents() {
        TeamTown town = townSource.get();
        if (town == null) return List.of();
        return List.copyOf(town.getAllResidents());
    }

    private static Line stat(String key, double value) {
        return new Line(Component.translatable(key)
                .append(Component.literal(": " + Math.round(value) + " / 100")), 0xFFFFFFFF);
    }

    private static List<VisualLine> wrapDetailLines(Font font, List<Line> lines) {
        List<VisualLine> wrapped = new ArrayList<>();
        for (Line line : lines) {
            for (FormattedCharSequence text : TownTextLayout.wrap(font, line.text(), DETAIL_TEXT_WIDTH)) {
                wrapped.add(new VisualLine(text, line.color()));
            }
        }
        return wrapped;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private record Line(Component text, int color) {}

    private record VisualLine(FormattedCharSequence text, int color) {}
}
