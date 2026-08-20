/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Always-visible, deliberately terse summary of the selected project. */
final class ResearchProjectSummaryPanel extends UIElement {
    private final ResearchWorkspaceState state;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private Map<String, Research> researchById = Map.of();

    ResearchProjectSummaryPanel(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent);
        this.state = Objects.requireNonNull(state, "state");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions) {
        Map<String, Research> byId = new HashMap<>();
        definitions.forEach(research -> byId.put(research.getId(), research));
        researchById = Map.copyOf(byId);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL);
        graphics.fill(x, y, x + 1, y + height, ResearchArchiveLayer.COLOR_PANEL_DARK);
        Research research = selectedResearch();
        if (research == null || !ResearchArchiveLayer.isDefinitionVisible(research)) {
            Component empty = Component.translatable("gui.frostedresearch.archive.select_project");
            drawWrapped(graphics, empty, x + 9, y + 12, width - 18,
                    ResearchArchiveLayer.COLOR_MUTED_INK, 3);
            return;
        }

        research.getIcon().draw(graphics, x + 9, y + 10, 18, 18);
        String title = getFont().plainSubstrByWidth(research.getName().getString(), width - 43);
        graphics.drawString(getFont(), title, x + 33, y + 11,
                ResearchArchiveLayer.COLOR_INK, false);
        String category = getFont().plainSubstrByWidth(research.getCategory().getName().getString(), width - 43);
        graphics.drawString(getFont(), category, x + 33, y + 22,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);

        int cursorY = y + 41;
        Component status = research.isCompleted()
                ? Component.translatable("gui.frostedresearch.archive.completed")
                : research.isInProgress()
                ? Component.translatable("gui.frostedresearch.research.in_progress")
                : research.isUnlocked()
                ? Component.translatable("gui.frostedresearch.research.can_research")
                : Component.translatable("gui.frostedresearch.archive.locked");
        int statusColor = research.isCompleted() ? ResearchArchiveLayer.COLOR_TEAL
                : research.isInProgress() ? ResearchArchiveLayer.COLOR_GOLD
                : research.isUnlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        graphics.drawString(getFont(), status, x + 9, cursorY, statusColor, false);
        cursorY += 14;

        float progress = Math.max(0.0F, Math.min(1.0F, research.getProgressFraction()));
        graphics.fill(x + 9, cursorY, x + width - 9, cursorY + 6,
                ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x + 9, cursorY,
                x + 9 + Math.round((width - 18) * progress), cursorY + 6,
                ResearchArchiveLayer.COLOR_TEAL);
        cursorY += 15;

        if (!research.getDesc().isEmpty()) {
            cursorY = drawWrapped(graphics, research.getDesc().get(0), x + 9, cursorY,
                    width - 18, ResearchArchiveLayer.COLOR_INK, 4);
        }
        int buttonY = y + height - 27;
        graphics.fill(x + 8, buttonY, x + width - 8, y + height - 7,
                ResearchArchiveLayer.COLOR_TEAL);
        Component details = Component.translatable("gui.frostedresearch.archive.open_details");
        String detailText = getFont().plainSubstrByWidth(details.getString(), Math.max(8, width - 20));
        int detailsX = x + Math.max(10, (width - getFont().width(detailText)) / 2);
        graphics.drawString(getFont(), detailText, detailsX, buttonY + 6, 0xFFF8F1DE, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        Research research = selectedResearch();
        if (!isMouseOver() || button != MouseButton.LEFT || research == null
                || !ResearchArchiveLayer.canReveal(research)) {
            return false;
        }
        if (getMouseY() >= getHeight() - 27) {
            navigation.openResearch(research.getId());
            navigationChanged.run();
            return true;
        }
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        Research research = selectedResearch();
        if (research != null && getMouseY() >= getHeight() - 27) {
            tooltip.accept(Component.translatable("gui.frostedresearch.archive.open_details"));
        }
    }

    @Override
    public Cursor getCursor() {
        return selectedResearch() != null && getMouseY() >= getHeight() - 27 ? Cursor.HAND : null;
    }

    private int drawWrapped(
            GuiGraphics graphics,
            Component text,
            int x,
            int y,
            int width,
            int color,
            int maxLines) {
        int count = 0;
        for (FormattedCharSequence line : getFont().split(text, width)) {
            if (count++ >= maxLines) {
                break;
            }
            graphics.drawString(getFont(), line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    @Nullable
    private Research selectedResearch() {
        return researchById.get(state.selectedResearchId());
    }
}
