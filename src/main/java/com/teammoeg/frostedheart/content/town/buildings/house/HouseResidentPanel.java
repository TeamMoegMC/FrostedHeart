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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class HouseResidentPanel extends UIElement {
    private static final int PANEL_WIDTH = 160;
    private static final int PANEL_HEIGHT = 130;
    private static final int LIST_WIDTH = 58;
    private static final int LIST_TOP = 20;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_RESIDENT_ROWS = 6;
    private static final int DETAIL_TOP = 6;
    private static final int DETAIL_LINE_HEIGHT = 13;
    private static final int DETAIL_VISIBLE_ROWS =
            (PANEL_HEIGHT - DETAIL_TOP * 2) / DETAIL_LINE_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int DETAIL_TEXT_WIDTH =
            PANEL_WIDTH - LIST_WIDTH - SCROLLBAR_WIDTH - 13;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final HouseMenu menu;
    private UUID selectedResident;
    private int residentScrollStart;
    private int detailScrollStart;
    private boolean isDetailScrollbarDragging;

    HouseResidentPanel(UIElement parent, int x, int y, HouseMenu menu) {
        super(parent);
        this.menu = menu;
        setPos(x, y);
        setSize(PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        List<Resident> residents = menu.getResidents();
        Resident selected = normalizeSelection(residents);

        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1, 0xFF777777);
        graphics.drawString(
                font,
                Component.translatable("gui.frostedheart.house.resident_list"),
                x + 4,
                y + 6,
                0xFFFFAA00,
                true);

        renderResidentList(graphics, font, x, y, residents);
        renderResidentDetails(graphics, font, x, y, buildResidentDetails(selected));
    }

    private void renderResidentList(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            List<Resident> residents
    ) {
        int maxScroll = Math.max(0, residents.size() - VISIBLE_RESIDENT_ROWS);
        residentScrollStart = Mth.clamp(residentScrollStart, 0, maxScroll);

        for (int row = 0; row < VISIBLE_RESIDENT_ROWS; row++) {
            int index = residentScrollStart + row;
            if (index >= residents.size()) {
                break;
            }
            Resident resident = residents.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            if (resident.getUUID().equals(selectedResident)) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0xA0446688);
            } else if (getMouseX() >= 2
                    && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0x60444444);
            }
            String displayName = TownTextLayout.ellipsize(
                    font, resident.toString(), LIST_WIDTH - 9);
            graphics.drawString(font, displayName, x + 5, rowY + 4, 0xFFFFFFFF, true);
        }

        if (maxScroll > 0) {
            int trackTop = y + LIST_TOP;
            int trackHeight = VISIBLE_RESIDENT_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(
                    MIN_THUMB_HEIGHT,
                    trackHeight * VISIBLE_RESIDENT_ROWS / residents.size());
            int thumbTravel = trackHeight - thumbHeight;
            int thumbY = trackTop + thumbTravel * residentScrollStart / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackTop, x + LIST_WIDTH - 1, trackTop + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY, x + LIST_WIDTH - 1, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderResidentDetails(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            List<DetailLine> lines
    ) {
        List<VisualDetailLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - DETAIL_VISIBLE_ROWS);
        detailScrollStart = Mth.clamp(detailScrollStart, 0, maxScroll);
        int textX = x + LIST_WIDTH + 5;
        for (int row = 0; row < DETAIL_VISIBLE_ROWS; row++) {
            int lineIndex = detailScrollStart + row;
            if (lineIndex >= visualLines.size()) {
                break;
            }
            VisualDetailLine line = visualLines.get(lineIndex);
            graphics.drawString(
                    font, line.text(), textX,
                    y + DETAIL_TOP + row * DETAIL_LINE_HEIGHT,
                    line.color(), true);
        }
        renderDetailScrollbar(graphics, x, y, visualLines.size(), maxScroll);
    }

    private List<DetailLine> buildResidentDetails(Resident resident) {
        List<DetailLine> lines = new ArrayList<>();
        if (resident == null) {
            lines.add(new DetailLine(
                    Component.translatable("gui.frostedheart.house.no_residents"),
                    0xFFAAAAAA));
            return lines;
        }

        lines.add(new DetailLine(Component.literal(resident.toString()), 0xFFFFAA00));
        lines.add(statLine("gui.frostedheart.house.health", resident.getHealth(), 100.0));
        lines.add(statLine("gui.frostedheart.house.mental", resident.getMental(), 100.0));
        lines.add(statLine("gui.frostedheart.house.strength", resident.getStrength(), 100.0));
        lines.add(statLine("gui.frostedheart.house.intelligence", resident.getIntelligence(), 100.0));
        lines.add(DetailLine.EMPTY);
        lines.add(new DetailLine(
                Component.translatable("gui.frostedheart.house.proficiency"),
                0xFFFFFF55));
        lines.add(statLine(
                "gui.frostedheart.house.mining",
                resident.getWorkProficiency(MineBaseBuilding.class),
                Resident.MAX_WORK_PROFICIENCY));
        lines.add(statLine(
                "gui.frostedheart.house.hunting",
                resident.getWorkProficiency(HuntingBaseBuilding.class),
                Resident.MAX_WORK_PROFICIENCY));
        lines.add(DetailLine.EMPTY);

        HouseBuilding house = menu.getHouse().orElse(null);
        if (house == null || !house.getDailyReport().hasData()) {
            lines.add(new DetailLine(
                    Component.translatable("gui.frostedheart.house.no_forecast"),
                    0xFFAAAAAA));
            return lines;
        }

        HouseDailyModel.ResidentEffects effects = house.calculateResidentEffects(resident);
        lines.add(new DetailLine(
                Component.translatable("gui.frostedheart.house.next_day_forecast"),
                0xFFFFFF55));
        lines.add(deltaLine("gui.frostedheart.house.health", effects.healthDelta()));
        lines.add(deltaLine("gui.frostedheart.house.mental", effects.mentalDelta()));
        return lines;
    }

    private Resident normalizeSelection(List<Resident> residents) {
        if (residents.isEmpty()) {
            selectedResident = null;
            residentScrollStart = 0;
            detailScrollStart = 0;
            return null;
        }
        for (Resident resident : residents) {
            if (resident.getUUID().equals(selectedResident)) {
                return resident;
            }
        }
        selectedResident = residents.get(0).getUUID();
        detailScrollStart = 0;
        return residents.get(0);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) {
            return false;
        }
        if (getMouseX() >= getWidth() - SCROLLBAR_WIDTH - 2 && hasScrollableDetails()) {
            isDetailScrollbarDragging = true;
            updateDetailScrollFromMouse();
            return true;
        }
        if (getMouseX() >= LIST_WIDTH) {
            return false;
        }
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (getMouseY() < LIST_TOP || row < 0 || row >= VISIBLE_RESIDENT_ROWS) {
            return false;
        }
        List<Resident> residents = menu.getResidents();
        int index = residentScrollStart + row;
        if (index >= residents.size()) {
            return false;
        }
        selectedResident = residents.get(index).getUUID();
        detailScrollStart = 0;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) {
            return false;
        }
        if (getMouseX() < LIST_WIDTH) {
            int maxScroll = Math.max(0, menu.getResidents().size() - VISIBLE_RESIDENT_ROWS);
            if (maxScroll == 0) {
                return false;
            }
            residentScrollStart = Mth.clamp(
                    residentScrollStart - (int) Math.signum(scroll),
                    0,
                    maxScroll);
            return true;
        }
        List<VisualDetailLine> lines =
                wrappedDetailLines(normalizeSelection(menu.getResidents()));
        int maxScroll = Math.max(0, lines.size() - DETAIL_VISIBLE_ROWS);
        if (maxScroll == 0) {
            return false;
        }
        detailScrollStart = Mth.clamp(
                detailScrollStart - (int) Math.signum(scroll),
                0,
                maxScroll);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (isDetailScrollbarDragging) {
            updateDetailScrollFromMouse();
            return true;
        }
        return super.onMouseDragged(button, dragX, dragY);
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        isDetailScrollbarDragging = false;
        super.onMouseReleased(button);
    }

    private void renderDetailScrollbar(
            GuiGraphics graphics,
            int x,
            int y,
            int totalLines,
            int maxScroll
    ) {
        if (maxScroll == 0) {
            detailScrollStart = 0;
            return;
        }
        int trackX = x + PANEL_WIDTH - SCROLLBAR_WIDTH - 2;
        int trackY = y + 2;
        int trackHeight = PANEL_HEIGHT - 4;
        int thumbHeight = getDetailThumbHeight(totalLines, trackHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = trackY + thumbTravel * detailScrollStart / maxScroll;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
        graphics.fill(
                trackX + 1,
                thumbY,
                trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight,
                isDetailScrollbarDragging ? 0xFFD0D0D0 : 0xFF909090);
    }

    private void updateDetailScrollFromMouse() {
        List<VisualDetailLine> lines =
                wrappedDetailLines(normalizeSelection(menu.getResidents()));
        int maxScroll = Math.max(0, lines.size() - DETAIL_VISIBLE_ROWS);
        if (maxScroll == 0) {
            detailScrollStart = 0;
            return;
        }
        int trackHeight = PANEL_HEIGHT - 4;
        int thumbHeight = getDetailThumbHeight(lines.size(), trackHeight);
        int thumbTravel = trackHeight - thumbHeight;
        if (thumbTravel <= 0) {
            detailScrollStart = 0;
            return;
        }
        double thumbTop = getMouseY() - 2 - thumbHeight / 2.0;
        double scrollRatio = Mth.clamp(thumbTop / thumbTravel, 0.0, 1.0);
        detailScrollStart = (int) Math.round(scrollRatio * maxScroll);
    }

    private boolean hasScrollableDetails() {
        return wrappedDetailLines(normalizeSelection(menu.getResidents())).size()
                > DETAIL_VISIBLE_ROWS;
    }

    private List<VisualDetailLine> wrappedDetailLines(Resident resident) {
        return wrapDetailLines(Minecraft.getInstance().font, buildResidentDetails(resident));
    }

    private static List<VisualDetailLine> wrapDetailLines(
            Font font, List<DetailLine> lines
    ) {
        List<VisualDetailLine> wrapped = new ArrayList<>();
        for (DetailLine line : lines) {
            for (FormattedCharSequence text :
                    TownTextLayout.wrap(font, line.text(), DETAIL_TEXT_WIDTH)) {
                wrapped.add(new VisualDetailLine(text, line.color()));
            }
        }
        return wrapped;
    }

    private static int getDetailThumbHeight(int totalLines, int trackHeight) {
        return Math.max(
                MIN_THUMB_HEIGHT,
                trackHeight * DETAIL_VISIBLE_ROWS / totalLines);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private static DetailLine statLine(String key, double value, double maximum) {
        return new DetailLine(
                Component.translatable(key)
                        .append(Component.literal(
                                ": " + Math.round(value) + " / " + Math.round(maximum))),
                0xFFFFFFFF);
    }

    private static DetailLine deltaLine(String key, double delta) {
        int color = delta > 0.0001 ? 0xFF55FF55 : delta < -0.0001 ? 0xFFFF5555 : 0xFFAAAAAA;
        return new DetailLine(
                Component.translatable(key)
                        .append(Component.literal(": " + signed(delta))),
                color);
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private record DetailLine(Component text, int color) {
        private static final DetailLine EMPTY =
                new DetailLine(Component.empty(), 0xFFFFFFFF);
    }

    private record VisualDetailLine(FormattedCharSequence text, int color) {}
}
