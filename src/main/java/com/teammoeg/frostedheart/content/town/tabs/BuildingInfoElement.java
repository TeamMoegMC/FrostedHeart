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

package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

public class BuildingInfoElement extends UIElement {

    private final Supplier<List<Component>> lineSource;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 5;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int MIN_THUMB_HEIGHT = 10;
    private float scrollOffset;
    private boolean isScrolling;

    public BuildingInfoElement(UIElement parent, int x, int y, int width, int height,
                               Supplier<List<Component>> lineSource) {
        super(parent);
        this.lineSource = lineSource;
        this.setPos(x, y);
        this.setSize(width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int w, int h, RenderingHint hint) {
        Minecraft mc = Minecraft.getInstance();
        List<FormattedCharSequence> lines = wrappedLines(mc.font, w);
        if (lines == null || lines.isEmpty()) return;

        guiGraphics.fill(x, y, x + w, y + h, 0xC0101010);


        guiGraphics.fill(x, y, x + w, y + 1, 0xFF373737);
        guiGraphics.fill(x, y, x + 1, y + h, 0xFF373737);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFF8B8B8B);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFF8B8B8B);


        int visibleLines = getVisibleLineCount();
        int hiddenLines = Math.max(0, lines.size() - visibleLines);
        int startLine = Math.round(scrollOffset * hiddenLines);
        int textRight = x + w - SCROLLBAR_WIDTH - 3;
        guiGraphics.enableScissor(x + 1, y + 1, textRight, y + h - 1);
        for (int row = 0; row < visibleLines; row++) {
            int lineIndex = startLine + row;
            if (lineIndex >= lines.size()) break;
            int textY = y + PADDING_Y + row * LINE_HEIGHT;
            guiGraphics.drawString(mc.font, lines.get(lineIndex),
                    x + PADDING_X, textY, 0xFFFFFFFF, true);
        }
        guiGraphics.disableScissor();

        renderScrollBar(guiGraphics, x, y, w, h, lines.size(), visibleLines);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT || !hasScrollableContent()) {
            return false;
        }
        if (getMouseX() >= getWidth() - SCROLLBAR_WIDTH - 2) {
            isScrolling = true;
            updateScrollFromMouse();
            return true;
        }
        return super.onMousePressed(button);
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        isScrolling = false;
        super.onMouseReleased(button);
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (isScrolling) {
            updateScrollFromMouse();
            return true;
        }
        return super.onMouseDragged(button, dragX, dragY);
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) {
            return false;
        }
        List<FormattedCharSequence> lines = wrappedLines();
        int hiddenLines = Math.max(0, lines.size() - getVisibleLineCount());
        if (hiddenLines == 0) {
            return false;
        }
        scrollOffset = Mth.clamp(
                scrollOffset - (float) Math.signum(scroll) / hiddenLines,
                0.0f,
                1.0f);
        return true;
    }

    private void renderScrollBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int totalLines,
            int visibleLines
    ) {
        if (totalLines <= visibleLines) {
            scrollOffset = 0.0f;
            return;
        }
        int trackX = x + width - SCROLLBAR_WIDTH - 2;
        int trackY = y + 2;
        int trackHeight = height - 4;
        int thumbHeight = getThumbHeight(totalLines, visibleLines, trackHeight);
        int thumbY = trackY + (int) (scrollOffset * (trackHeight - thumbHeight));
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
        graphics.fill(
                trackX + 1,
                thumbY,
                trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight,
                isScrolling ? 0xFFD0D0D0 : 0xFF909090);
    }

    private void updateScrollFromMouse() {
        int totalLines = wrappedLines().size();
        int visibleLines = getVisibleLineCount();
        if (totalLines <= visibleLines) {
            scrollOffset = 0.0f;
            return;
        }
        int trackHeight = getHeight() - 4;
        int thumbHeight = getThumbHeight(totalLines, visibleLines, trackHeight);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) {
            scrollOffset = 0.0f;
            return;
        }
        double thumbTop = getMouseY() - 2 - thumbHeight / 2.0;
        scrollOffset = Mth.clamp((float) (thumbTop / travel), 0.0f, 1.0f);
    }

    private int getVisibleLineCount() {
        return Math.max(1, (getHeight() - PADDING_Y * 2) / LINE_HEIGHT);
    }

    private boolean hasScrollableContent() {
        return wrappedLines().size() > getVisibleLineCount();
    }

    private List<FormattedCharSequence> wrappedLines() {
        return wrappedLines(Minecraft.getInstance().font, getWidth());
    }

    private List<FormattedCharSequence> wrappedLines(Font font, int panelWidth) {
        List<Component> sourceLines = lineSource.get();
        if (sourceLines == null || sourceLines.isEmpty()) return List.of();
        int availableWidth = Math.max(
                1, panelWidth - PADDING_X * 2 - SCROLLBAR_WIDTH - 3);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component sourceLine : sourceLines) {
            wrapped.addAll(TownTextLayout.wrap(font, sourceLine, availableWidth));
        }
        return wrapped;
    }

    private static int getThumbHeight(int totalLines, int visibleLines, int trackHeight) {
        return Math.max(MIN_THUMB_HEIGHT, trackHeight * visibleLines / totalLines);
    }

    public static Component title(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    public static Component separator() {
        return Component.literal("───────────────").withStyle(ChatFormatting.DARK_GRAY);
    }

    public static Component keyValue(String label, Object value) {
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(String.valueOf(value));
    }


    public static Component status(String label, boolean value) {
        ChatFormatting color = value ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(label + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(String.valueOf(value)).withStyle(color));
    }

    public static Component number(String label, double value) {
        String formatted = BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue() + "";
        return keyValue(label, formatted);
    }
}
