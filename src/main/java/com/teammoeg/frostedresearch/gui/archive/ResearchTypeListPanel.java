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
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Compact searchable project index below the archive search box. */
final class ResearchTypeListPanel extends UIElement {
    private static final int HEADER_HEIGHT = 18;
    private static final int RESEARCH_ROW_HEIGHT = 19;
    private static final Component PROJECTS =
            Component.translatable("gui.frostedresearch.archive.projects");

    private final ResearchWorkspaceState state;
    private final ResearchArchiveViewCache viewCache;
    private final Runnable navigationChanged;
    private final boolean ownsViewCache;
    private List<Research> definitions = List.of();
    private List<Research> visibleResearchCache = List.of();
    @Nullable
    private VisibleResearchCacheKey visibleResearchCacheKey;
    private boolean visibleResearchesDirty = true;
    private int visibleResearchBuildCount;
    private final Map<String, RowTitles> rowTitlesById = new HashMap<>();
    @Nullable
    private RowTitleCacheKey rowTitleCacheKey;

    ResearchTypeListPanel(
            ResearchArchiveLayer parent,
            ResearchWorkspaceState state,
            Runnable navigationChanged) {
        this(parent, state, new ResearchArchiveViewCache(), navigationChanged, true);
    }

    ResearchTypeListPanel(
            ResearchArchiveLayer parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            Runnable navigationChanged) {
        this(parent, state, viewCache, navigationChanged, false);
    }

    private ResearchTypeListPanel(
            ResearchArchiveLayer parent,
            ResearchWorkspaceState state,
            ResearchArchiveViewCache viewCache,
            Runnable navigationChanged,
            boolean ownsViewCache) {
        super(parent);
        this.state = Objects.requireNonNull(state, "state");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
        this.ownsViewCache = ownsViewCache;
    }

    void setDefinitions(List<Research> definitions) {
        this.definitions = List.copyOf(definitions);
        if (ownsViewCache) {
            viewCache.setDefinitions(definitions);
        }
        invalidateVisibleResearches();
        invalidateRowTitles();
        clampScroll();
    }

    void onFilterChanged() {
        invalidateVisibleResearches();
        clampScroll();
    }

    void onProgressChanged(@Nullable String researchId) {
        invalidateVisibleResearches();
        clampScroll();
    }

    void onActiveResearchChanged() {
        // Active research changes row styling, not the legacy list order.
    }

    void onPresentationChanged() {
        invalidateVisibleResearches();
        invalidateRowTitles();
        clampScroll();
    }

    void onBookmarksChanged() {
        // Bookmarks remain row markers and do not alter the legacy list order.
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL);
        graphics.fill(x, y, x + width, y + 1, ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL_DARK);

        graphics.fill(x + 1, y + 1, x + width - 1, y + HEADER_HEIGHT,
                ResearchArchiveLayer.COLOR_PAPER_DARK);
        graphics.drawString(getFont(), PROJECTS, x + 6, y + 6,
                ResearchArchiveLayer.COLOR_INK, false);
        int bookmarkCount = state.bookmarkedResearchIds().size();
        if (bookmarkCount > 0) {
            String marker = "*" + bookmarkCount;
            graphics.drawString(getFont(), marker, x + width - getFont().width(marker) - 6,
                    y + 6, ResearchArchiveLayer.COLOR_RED, false);
        }

        int listTop = y + HEADER_HEIGHT;
        graphics.enableScissor(x + 1, listTop, x + width - 1, y + height - 1);
        List<Research> visible = visibleResearches();
        Research hovered = isMouseOver() ? rowAtMouse(visible) : null;
        int scroll = state.typeListScroll(state.researchTypeFilter());
        int listHeight = Math.max(0, height - HEADER_HEIGHT);
        int firstRow = Math.max(0, scroll / RESEARCH_ROW_HEIGHT);
        int lastRowExclusive = Math.min(
                visible.size(),
                (scroll + listHeight + RESEARCH_ROW_HEIGHT - 1) / RESEARCH_ROW_HEIGHT);
        int rowY = listTop + firstRow * RESEARCH_ROW_HEIGHT - scroll;
        String currentId = currentResearchId();
        Set<String> bookmarks = state.bookmarkedResearchIds();
        prepareRowTitleCache(width - 2);
        for (int row = firstRow; row < lastRowExclusive; row++) {
            Research research = visible.get(row);
            drawResearchRow(graphics, research, hovered, currentId, bookmarks, x + 1, rowY, width - 2);
            rowY += RESEARCH_ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private void drawResearchRow(
            GuiGraphics graphics,
            Research research,
            @Nullable Research hovered,
            @Nullable String currentId,
            Set<String> bookmarks,
            int x,
            int y,
            int width) {
        boolean selected = research.getId().equals(state.selectedResearchId());
        boolean active = research.getId().equals(currentId);
        boolean bookmarked = bookmarks.contains(research.getId());
        ResearchArchiveViewCache.View view = viewCache.view(research.getId());
        if (view == null) {
            return;
        }
        if (selected) {
            graphics.fill(x, y, x + width, y + RESEARCH_ROW_HEIGHT - 1, 0x55FFFFFF);
        } else if (hovered == research) {
            graphics.fill(x, y, x + width, y + RESEARCH_ROW_HEIGHT - 1, 0x22FFFFFF);
        }
        int statusColor = view.completed()
                ? ResearchArchiveLayer.COLOR_TEAL
                : active ? ResearchArchiveLayer.COLOR_GOLD
                : view.unlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        graphics.fill(x + 4, y + 5, x + 8, y + 14, statusColor);
        if (bookmarked) {
            graphics.drawString(getFont(), "*", x + width - 9, y + 6,
                    ResearchArchiveLayer.COLOR_RED, false);
        }
        RowTitles titles = rowTitles(view, width);
        String title = bookmarked ? titles.bookmarked : titles.normal;
        graphics.drawString(getFont(), title, x + 12, y + 6,
                ResearchArchiveLayer.COLOR_INK, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) {
            return false;
        }
        Research research = rowAtMouse();
        if (research == null) {
            return true;
        }
        if (button == MouseButton.RIGHT) {
            boolean bookmarked = state.bookmarkedResearchIds().contains(research.getId());
            state.setBookmarked(research.getId(), !bookmarked);
            onBookmarksChanged();
            return true;
        }
        if (button == MouseButton.LEFT) {
            state.selectResearch(research.getId());
            state.setProjectWorkspaceOpen(false);
            navigationChanged.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) {
            return false;
        }
        int current = state.typeListScroll(state.researchTypeFilter());
        state.setTypeListScroll(state.researchTypeFilter(),
                current - (int) Math.signum(scroll) * RESEARCH_ROW_HEIGHT * 2);
        clampScroll();
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        Research research = rowAtMouse();
        if (research != null) {
            ResearchArchiveViewCache.View view = viewCache.view(research.getId());
            tooltip.accept(view == null ? research.getName() : view.name());
            if (FHResearch.editor) {
                tooltip.accept(Component.literal(research.getId()));
            }
            tooltip.accept(Component.translatable("gui.frostedresearch.archive.bookmark_hint"));
        }
    }

    @Override
    public Cursor getCursor() {
        return isMouseOver() ? Cursor.HAND : null;
    }

    @Nullable
    private Research rowAtMouse() {
        return rowAtMouse(visibleResearches());
    }

    @Nullable
    private Research rowAtMouse(List<Research> visible) {
        int listTop = HEADER_HEIGHT;
        int localY = (int) getMouseY();
        if (localY < listTop || localY >= getHeight()) {
            return null;
        }
        int index = (localY - listTop + state.typeListScroll(state.researchTypeFilter()))
                / RESEARCH_ROW_HEIGHT;
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    private List<Research> visibleResearches() {
        String filter = state.researchTypeFilter();
        String sourceQuery = state.searchQuery();
        long presentationRevision = viewCache.presentationRevision();
        if (!visibleResearchesDirty && visibleResearchCacheKey != null
                && visibleResearchCacheKey.matches(
                        filter, sourceQuery, FHResearch.editor, presentationRevision)) {
            return visibleResearchCache;
        }
        String query = sourceQuery.toLowerCase(Locale.ROOT);
        List<Research> visible = new ArrayList<>();
        for (Research research : definitions) {
            ResearchArchiveViewCache.View view = viewCache.view(research.getId());
            if (view == null) {
                continue;
            }
            if (!ResearchTypeIdNormalizer.ALL_TYPES.equals(filter)
                    && !ResearchTypeIdNormalizer.normalize(research.getCategory()).equals(filter)) {
                continue;
            }
            if (!query.isEmpty()
                    && !view.lowercaseSearchText().contains(query)) {
                continue;
            }
            visible.add(research);
        }
        visibleResearchCache = List.copyOf(
                FHResearch.getResearchesForRender(visible, FHResearch.editor));
        visibleResearchCacheKey = new VisibleResearchCacheKey(
                filter, sourceQuery, FHResearch.editor, presentationRevision);
        visibleResearchesDirty = false;
        visibleResearchBuildCount++;
        return visibleResearchCache;
    }

    private void invalidateVisibleResearches() {
        visibleResearchesDirty = true;
    }

    private void prepareRowTitleCache(int width) {
        long presentationRevision = viewCache.presentationRevision();
        if (rowTitleCacheKey != null && rowTitleCacheKey.matches(width, presentationRevision)) {
            return;
        }
        rowTitlesById.clear();
        rowTitleCacheKey = new RowTitleCacheKey(width, presentationRevision);
    }

    private RowTitles rowTitles(ResearchArchiveViewCache.View view, int width) {
        RowTitles titles = rowTitlesById.get(view.research().getId());
        if (titles != null) {
            return titles;
        }
        String title = view.title();
        titles = new RowTitles(
                getFont().plainSubstrByWidth(title, Math.max(1, width - 17)),
                getFont().plainSubstrByWidth(title, Math.max(1, width - 25)));
        rowTitlesById.put(view.research().getId(), titles);
        return titles;
    }

    private void invalidateRowTitles() {
        rowTitleCacheKey = null;
        rowTitlesById.clear();
    }

    private void clampScroll() {
        int listHeight = Math.max(0, getHeight() - HEADER_HEIGHT);
        int maxScroll = Math.max(0, visibleResearches().size() * RESEARCH_ROW_HEIGHT - listHeight);
        int current = state.typeListScroll(state.researchTypeFilter());
        if (current > maxScroll) {
            state.setTypeListScroll(state.researchTypeFilter(), maxScroll);
        }
    }

    @Nullable
    private String currentResearchId() {
        return viewCache.activeResearchId();
    }

    List<Research> visibleResearchesForTest() {
        return visibleResearches();
    }

    int visibleResearchBuildCountForTest() {
        return visibleResearchBuildCount;
    }

    private record VisibleResearchCacheKey(
            String researchType,
            String sourceQuery,
            boolean editor,
            long presentationRevision) {

        private boolean matches(
                String currentResearchType,
                String currentSourceQuery,
                boolean currentEditor,
                long currentPresentationRevision) {
            return researchType.equals(currentResearchType)
                    && sourceQuery.equals(currentSourceQuery)
                    && editor == currentEditor
                    && presentationRevision == currentPresentationRevision;
        }
    }

    private record RowTitleCacheKey(int width, long presentationRevision) {
        private boolean matches(int currentWidth, long currentPresentationRevision) {
            return width == currentWidth && presentationRevision == currentPresentationRevision;
        }
    }

    private record RowTitles(String normal, String bookmarked) {
    }
}
