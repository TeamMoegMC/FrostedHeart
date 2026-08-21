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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Always-visible, deliberately terse summary of the selected project. */
final class ResearchProjectSummaryPanel extends UIElement {
    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    private SummaryRenderData renderData;
    private String cachedResearchId;
    private int cachedWidth = -1;
    private long cachedPresentationRevision = -1L;
    private long cachedStateRevision = -1L;

    ResearchProjectSummaryPanel(
            UIElement parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            ResearchNavigationController navigation,
            Runnable navigationChanged) {
        super(parent);
        this.state = Objects.requireNonNull(state, "state");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions) {
        invalidateRenderData();
    }

    void onPresentationChanged() {
        invalidateRenderData();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL);
        graphics.fill(x, y, x + 1, y + height, ResearchArchiveLayer.COLOR_PANEL_DARK);
        SummaryRenderData data = renderData(width);
        if (data == null) {
            Component empty = Component.translatable("gui.frostedresearch.archive.select_project");
            drawWrapped(graphics, empty, x + 9, y + 12, width - 18,
                    ResearchArchiveLayer.COLOR_MUTED_INK, 3);
            return;
        }

        data.view.research().getIcon().draw(graphics, x + 9, y + 10, 18, 18);
        graphics.drawString(getFont(), data.title, x + 33, y + 11,
                ResearchArchiveLayer.COLOR_INK, false);
        graphics.drawString(getFont(), data.category, x + 33, y + 22,
                ResearchArchiveLayer.COLOR_MUTED_INK, false);

        int cursorY = y + 41;
        graphics.drawString(getFont(), data.status, x + 9, cursorY, data.statusColor, false);
        cursorY += 14;

        graphics.fill(x + 9, cursorY, x + width - 9, cursorY + 6,
                ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x + 9, cursorY,
                x + 9 + Math.round((width - 18) * data.view.progress()), cursorY + 6,
                ResearchArchiveLayer.COLOR_TEAL);
        cursorY += 15;

        for (FormattedCharSequence line : data.descriptionLines) {
            graphics.drawString(getFont(), line, x + 9, cursorY, ResearchArchiveLayer.COLOR_INK, false);
            cursorY += 11;
        }
        int buttonY = y + height - 27;
        graphics.fill(x + 8, buttonY, x + width - 8, y + height - 7,
                ResearchArchiveLayer.COLOR_TEAL);
        int detailsX = x + Math.max(10, (width - getFont().width(data.detailsText)) / 2);
        graphics.drawString(getFont(), data.detailsText, detailsX, buttonY + 6, 0xFFF8F1DE, false);
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
        ResearchArchiveViewCache.View view = viewCache.view(state.selectedResearchId());
        return view == null ? null : view.research();
    }

    @Nullable
    private SummaryRenderData renderData(int width) {
        String selectedId = state.selectedResearchId();
        if (renderData != null
                && Objects.equals(cachedResearchId, selectedId)
                && cachedWidth == width
                && cachedPresentationRevision == viewCache.presentationRevision()
                && cachedStateRevision == viewCache.stateRevision()) {
            return renderData;
        }
        cachedResearchId = selectedId;
        cachedWidth = width;
        cachedPresentationRevision = viewCache.presentationRevision();
        cachedStateRevision = viewCache.stateRevision();
        ResearchArchiveViewCache.View view = viewCache.view(selectedId);
        if (view == null) {
            renderData = null;
            return null;
        }
        String title = getFont().plainSubstrByWidth(view.title(), width - 43);
        String category = getFont().plainSubstrByWidth(view.categoryTitle(), width - 43);
        Component status = view.completed()
                ? Component.translatable("gui.frostedresearch.archive.completed")
                : view.active()
                ? Component.translatable("gui.frostedresearch.research.in_progress")
                : view.unlocked()
                ? Component.translatable("gui.frostedresearch.research.can_research")
                : Component.translatable("gui.frostedresearch.archive.locked");
        int statusColor = view.completed() ? ResearchArchiveLayer.COLOR_TEAL
                : view.active() ? ResearchArchiveLayer.COLOR_GOLD
                : view.unlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        List<FormattedCharSequence> descriptionLines = new ArrayList<>(4);
        List<Component> descriptions = view.research().getDesc();
        if (!descriptions.isEmpty()) {
            for (FormattedCharSequence line : getFont().split(descriptions.get(0), width - 18)) {
                if (descriptionLines.size() == 4) {
                    break;
                }
                descriptionLines.add(line);
            }
        }
        Component details = Component.translatable("gui.frostedresearch.archive.open_details");
        String detailsText = getFont().plainSubstrByWidth(details.getString(), Math.max(8, width - 20));
        renderData = new SummaryRenderData(
                view, title, category, status, statusColor, List.copyOf(descriptionLines), detailsText);
        return renderData;
    }

    private void invalidateRenderData() {
        cachedPresentationRevision = -1L;
        cachedStateRevision = -1L;
        renderData = null;
    }

    private record SummaryRenderData(
            ResearchArchiveViewCache.View view,
            String title,
            String category,
            Component status,
            int statusColor,
            List<FormattedCharSequence> descriptionLines,
            String detailsText) {
    }
}
