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
 */

package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Compact read-only information panel with optional item icons and a draggable
 * scrollbar. All three town production screens use the same row geometry.
 */
public class TownInfoPanel extends UIElement {
    public record Row(
            Component text,
            int color,
            @Nullable ItemStack icon,
            @Nullable Component tooltip,
            @Nullable Runnable clickAction
    ) {
        public Row(Component text, int color, @Nullable ItemStack icon) {
            this(text, color, icon, null, null);
        }

        public static Row text(Component text) {
            return new Row(text, 0xFFFFFFFF, null, null, null);
        }

        public static Row colored(Component text, int color) {
            return new Row(text, color, null, null, null);
        }

        public static Row item(Item item, Component text) {
            return new Row(text, 0xFFFFFFFF, new ItemStack(item), null, null);
        }

        public static Row empty() {
            return new Row(Component.empty(), 0xFFFFFFFF, null, null, null);
        }

        public static Row clickable(Component text, int color, Runnable clickAction) {
            return new Row(text, color, null, null, clickAction);
        }

        public Row withTooltip(Component tooltip) {
            return new Row(text, color, icon, tooltip, clickAction);
        }
    }

    private static final int LINE_HEIGHT = 14;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 4;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<List<Row>> rowSource;
    private final boolean drawFrame;
    private int scrollStart;
    private boolean draggingScrollbar;

    private record VisualRow(
            FormattedCharSequence text,
            int color,
            @Nullable ItemStack icon,
            int textIndent,
            @Nullable Component tooltip,
            @Nullable Runnable clickAction
    ) {}

    public TownInfoPanel(
            UIElement parent, int x, int y, int width, int height,
            Supplier<List<Row>> rowSource
    ) {
        this(parent, x, y, width, height, rowSource, true);
    }

    public TownInfoPanel(
            UIElement parent, int x, int y, int width, int height,
            Supplier<List<Row>> rowSource, boolean drawFrame
    ) {
        super(parent);
        this.rowSource = rowSource;
        this.drawFrame = drawFrame;
        setPos(x, y);
        setSize(width, height);
    }

    /** Returns the panel to its first row after its owning view changes context. */
    public void resetScroll() {
        scrollStart = 0;
        draggingScrollbar = false;
    }

    @Override
    public void render(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        List<VisualRow> rows = visualRows(font, width);
        if (drawFrame) drawPanel(graphics, x, y, width, height);

        int visible = visibleRows();
        int maxScroll = Math.max(0, rows.size() - visible);
        scrollStart = Mth.clamp(scrollStart, 0, maxScroll);
        int textRight = x + width - SCROLLBAR_WIDTH - 4;
        graphics.enableScissor(x + 1, y + 1, textRight, y + height - 1);
        for (int rowIndex = 0; rowIndex < visible; rowIndex++) {
            int index = scrollStart + rowIndex;
            if (index >= rows.size()) break;
            VisualRow row = rows.get(index);
            int rowY = y + PADDING_Y + rowIndex * LINE_HEIGHT;
            int textX = x + PADDING_X + row.textIndent();
            if (row.icon() != null && !row.icon().isEmpty()) {
                CIcons.getIcon(row.icon()).draw(
                        graphics, x + PADDING_X, rowY - 2, 12, 12);
            }
            graphics.drawString(font, row.text(), textX, rowY, row.color(), true);
        }
        graphics.disableScissor();
        renderScrollbar(graphics, x, y, width, height, rows.size(), visible, maxScroll);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (isScrollable() && getMouseX() >= getWidth() - SCROLLBAR_WIDTH - 2) {
            draggingScrollbar = true;
            updateScrollFromMouse();
            return true;
        }
        int localY = (int) getMouseY() - PADDING_Y;
        if (localY >= 0) {
            List<VisualRow> rows = visualRows();
            int index = scrollStart + localY / LINE_HEIGHT;
            if (index >= 0 && index < rows.size() && rows.get(index).clickAction() != null) {
                rows.get(index).clickAction().run();
                return true;
            }
        }
        return super.onMousePressed(button);
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        draggingScrollbar = false;
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!draggingScrollbar) return super.onMouseDragged(button, dragX, dragY);
        updateScrollFromMouse();
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        int maxScroll = Math.max(0, visualRows().size() - visibleRows());
        if (maxScroll == 0) return false;
        scrollStart = Mth.clamp(
                scrollStart - (int) Math.signum(scroll), 0, maxScroll);
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (!isMouseOver()) return;
        int localY = (int) getMouseY() - PADDING_Y;
        if (localY < 0) return;
        List<VisualRow> rows = visualRows();
        int index = scrollStart + localY / LINE_HEIGHT;
        if (index < 0 || index >= rows.size()) return;
        Component rowTooltip = rows.get(index).tooltip();
        if (rowTooltip != null) tooltip.accept(rowTooltip);
    }

    private void renderScrollbar(
            GuiGraphics graphics, int x, int y, int width, int height,
            int totalRows, int visibleRows, int maxScroll
    ) {
        if (maxScroll <= 0) {
            scrollStart = 0;
            return;
        }
        int trackX = x + width - SCROLLBAR_WIDTH - 2;
        int trackY = y + 2;
        int trackHeight = height - 4;
        int thumbHeight = thumbHeight(totalRows, visibleRows, trackHeight);
        int travel = trackHeight - thumbHeight;
        int thumbY = trackY + travel * scrollStart / maxScroll;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
        graphics.fill(
                trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                draggingScrollbar ? 0xFFD0D0D0 : 0xFF909090);
    }

    private void updateScrollFromMouse() {
        List<VisualRow> rows = visualRows();
        int visible = visibleRows();
        int maxScroll = Math.max(0, rows.size() - visible);
        if (maxScroll == 0) {
            scrollStart = 0;
            return;
        }
        int trackHeight = getHeight() - 4;
        int thumbHeight = thumbHeight(rows.size(), visible, trackHeight);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) {
            scrollStart = 0;
            return;
        }
        double top = getMouseY() - 2 - thumbHeight / 2.0;
        scrollStart = (int) Math.round(Mth.clamp(top / travel, 0.0, 1.0) * maxScroll);
    }

    private boolean isScrollable() {
        return visualRows().size() > visibleRows();
    }

    private int visibleRows() {
        return Math.max(1, (getHeight() - PADDING_Y * 2) / LINE_HEIGHT);
    }

    private List<Row> safeRows() {
        List<Row> rows = rowSource.get();
        return rows == null ? List.of() : rows;
    }

    private List<VisualRow> visualRows() {
        return visualRows(Minecraft.getInstance().font, getWidth());
    }

    private List<VisualRow> visualRows(Font font, int panelWidth) {
        List<VisualRow> visualRows = new ArrayList<>();
        int textRight = panelWidth - SCROLLBAR_WIDTH - 4;
        for (Row row : safeRows()) {
            boolean hasIcon = row.icon() != null && !row.icon().isEmpty();
            int textIndent = hasIcon ? 15 : 0;
            int availableWidth = Math.max(
                    1, textRight - PADDING_X - textIndent - 2);
            List<FormattedCharSequence> wrapped =
                    TownTextLayout.wrap(font, row.text(), availableWidth);
            for (int index = 0; index < wrapped.size(); index++) {
                visualRows.add(new VisualRow(
                        wrapped.get(index),
                        row.color(),
                        hasIcon && index == 0 ? row.icon() : null,
                        textIndent,
                        row.tooltip(),
                        row.clickAction()
                ));
            }
        }
        return visualRows;
    }

    private static int thumbHeight(int total, int visible, int trackHeight) {
        return Math.max(MIN_THUMB_HEIGHT, trackHeight * visible / total);
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
}
