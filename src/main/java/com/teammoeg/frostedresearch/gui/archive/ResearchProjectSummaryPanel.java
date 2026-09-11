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
    private static final Component SELECT_PROJECT =
            Component.translatable("gui.frostedresearch.archive.select_project");
    private static final Component COMPLETED =
            Component.translatable("gui.frostedresearch.archive.completed");
    private static final Component IN_PROGRESS =
            Component.translatable("gui.frostedresearch.research.in_progress");
    private static final Component CAN_RESEARCH =
            Component.translatable("gui.frostedresearch.research.can_research");
    private static final Component LOCKED =
            Component.translatable("gui.frostedresearch.archive.locked");
    private static final Component OPEN_DETAILS =
            Component.translatable("gui.frostedresearch.archive.open_details");

    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchNavigationController navigation;
    private final Runnable navigationChanged;
    @Nullable
    private SummaryCacheKey summaryCacheKey;
    @Nullable
    private SummaryRenderData cachedSummary;
    private List<FormattedCharSequence> cachedEmptyLines = List.of();
    @Nullable
    private TextWrapCacheKey emptyTextCacheKey;

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
        SummaryRenderData data = summaryForWidth(width);
        if (data == null) {
            int lineY = y + 12;
            for (FormattedCharSequence line : emptyLines(width - 18)) {
                graphics.drawString(getFont(), line, x + 9, lineY,
                        ResearchArchiveLayer.COLOR_MUTED_INK, false);
                lineY += 11;
            }
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
        graphics.drawString(getFont(), data.detailsText, x + data.detailsXOffset,
                buttonY + 6, 0xFFF8F1DE, false);
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
            tooltip.accept(OPEN_DETAILS);
        }
    }

    @Override
    public Cursor getCursor() {
        return selectedResearch() != null && getMouseY() >= getHeight() - 27 ? Cursor.HAND : null;
    }

    @Nullable
    private Research selectedResearch() {
        ResearchArchiveViewCache.View view = viewCache.view(state.selectedResearchId());
        return view == null ? null : view.research();
    }

    @Nullable
    private SummaryRenderData summaryForWidth(int width) {
        String selectedId = state.selectedResearchId();
        long presentationRevision = viewCache.presentationRevision();
        long stateRevision = viewCache.stateRevision();
        if (summaryCacheKey != null
                && summaryCacheKey.matches(selectedId, width, presentationRevision, stateRevision)) {
            return cachedSummary;
        }
        summaryCacheKey = new SummaryCacheKey(
                selectedId, width, presentationRevision, stateRevision);
        ResearchArchiveViewCache.View view = viewCache.view(selectedId);
        if (view == null) {
            cachedSummary = null;
            return null;
        }
        int headerWidth = Math.max(1, width - 43);
        String title = getFont().plainSubstrByWidth(view.title(), headerWidth);
        String category = getFont().plainSubstrByWidth(view.categoryTitle(), headerWidth);
        Component status = view.completed()
                ? COMPLETED
                : view.active()
                ? IN_PROGRESS
                : view.unlocked()
                ? CAN_RESEARCH
                : LOCKED;
        int statusColor = view.completed() ? ResearchArchiveLayer.COLOR_TEAL
                : view.active() ? ResearchArchiveLayer.COLOR_GOLD
                : view.unlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        List<FormattedCharSequence> descriptionLines = new ArrayList<>(4);
        List<Component> descriptions = view.research().getDesc();
        if (!descriptions.isEmpty()) {
            for (FormattedCharSequence line : getFont().split(
                    descriptions.get(0), Math.max(1, width - 18))) {
                if (descriptionLines.size() == 4) {
                    break;
                }
                descriptionLines.add(line);
            }
        }
        String detailsText = getFont().plainSubstrByWidth(
                OPEN_DETAILS.getString(), Math.max(8, width - 20));
        int detailsXOffset = Math.max(10, (width - getFont().width(detailsText)) / 2);
        cachedSummary = new SummaryRenderData(
                view, title, category, status, statusColor,
                List.copyOf(descriptionLines), detailsText, detailsXOffset);
        return cachedSummary;
    }

    private List<FormattedCharSequence> emptyLines(int width) {
        int safeWidth = Math.max(1, width);
        long presentationRevision = viewCache.presentationRevision();
        if (emptyTextCacheKey != null
                && emptyTextCacheKey.matches(safeWidth, presentationRevision)) {
            return cachedEmptyLines;
        }
        List<FormattedCharSequence> lines = new ArrayList<>(3);
        for (FormattedCharSequence line : getFont().split(SELECT_PROJECT, safeWidth)) {
            if (lines.size() == 3) {
                break;
            }
            lines.add(line);
        }
        cachedEmptyLines = List.copyOf(lines);
        emptyTextCacheKey = new TextWrapCacheKey(safeWidth, presentationRevision);
        return cachedEmptyLines;
    }

    private void invalidateRenderData() {
        summaryCacheKey = null;
        cachedSummary = null;
        emptyTextCacheKey = null;
    }

    private record SummaryCacheKey(
            @Nullable String researchId,
            int width,
            long presentationRevision,
            long stateRevision) {

        private boolean matches(
                @Nullable String selectedResearchId,
                int currentWidth,
                long currentPresentationRevision,
                long currentStateRevision) {
            return Objects.equals(researchId, selectedResearchId)
                    && width == currentWidth
                    && presentationRevision == currentPresentationRevision
                    && stateRevision == currentStateRevision;
        }
    }

    private record TextWrapCacheKey(int width, long presentationRevision) {
        private boolean matches(int currentWidth, long currentPresentationRevision) {
            return width == currentWidth && presentationRevision == currentPresentationRevision;
        }
    }

    private record SummaryRenderData(
            ResearchArchiveViewCache.View view,
            String title,
            String category,
            Component status,
            int statusColor,
            List<FormattedCharSequence> descriptionLines,
            String detailsText,
            int detailsXOffset) {
    }
}
