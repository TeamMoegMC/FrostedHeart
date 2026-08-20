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
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Compact searchable project index below the archive search box. */
final class ResearchTypeListPanel extends UIElement {
    private static final int HEADER_HEIGHT = 18;
    private static final int RESEARCH_ROW_HEIGHT = 19;

    private final ResearchWorkspaceState state;
    private final Runnable navigationChanged;
    private List<Research> definitions = List.of();

    ResearchTypeListPanel(
            ResearchArchiveLayer parent,
            ResearchWorkspaceState state,
            Runnable navigationChanged) {
        super(parent);
        this.state = Objects.requireNonNull(state, "state");
        this.navigationChanged = Objects.requireNonNull(navigationChanged, "navigationChanged");
    }

    void setDefinitions(List<Research> definitions) {
        this.definitions = List.copyOf(definitions);
        clampScroll();
    }

    void onFilterChanged() {
        clampScroll();
    }

    void onProgressChanged(@Nullable String researchId) {
        // Progress is read live while rendering; only the ordering may change.
        clampScroll();
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        graphics.fill(x, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL);
        graphics.fill(x, y, x + width, y + 1, ResearchArchiveLayer.COLOR_PANEL_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, ResearchArchiveLayer.COLOR_PANEL_DARK);

        graphics.fill(x + 1, y + 1, x + width - 1, y + HEADER_HEIGHT,
                ResearchArchiveLayer.COLOR_PAPER_DARK);
        Component projects = Component.translatable("gui.frostedresearch.archive.projects");
        graphics.drawString(getFont(), projects, x + 6, y + 6,
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
        int scroll = state.typeListScroll(state.researchTypeFilter());
        int rowY = listTop - scroll;
        String currentId = currentResearchId();
        for (Research research : visible) {
            if (rowY + RESEARCH_ROW_HEIGHT >= listTop && rowY < y + height) {
                drawResearchRow(graphics, research, currentId, x + 1, rowY, width - 2);
            }
            rowY += RESEARCH_ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private void drawResearchRow(
            GuiGraphics graphics,
            Research research,
            @Nullable String currentId,
            int x,
            int y,
            int width) {
        boolean selected = research.getId().equals(state.selectedResearchId());
        boolean active = research.getId().equals(currentId);
        boolean bookmarked = state.bookmarkedResearchIds().contains(research.getId());
        if (selected) {
            graphics.fill(x, y, x + width, y + RESEARCH_ROW_HEIGHT - 1, 0x55FFFFFF);
        } else if (isMouseOver() && rowAtMouse() == research) {
            graphics.fill(x, y, x + width, y + RESEARCH_ROW_HEIGHT - 1, 0x22FFFFFF);
        }
        int statusColor = research.isCompleted()
                ? ResearchArchiveLayer.COLOR_TEAL
                : active ? ResearchArchiveLayer.COLOR_GOLD
                : research.isUnlocked() ? ResearchArchiveLayer.COLOR_RED
                : ResearchArchiveLayer.COLOR_MUTED_INK;
        graphics.fill(x + 4, y + 5, x + 8, y + 14, statusColor);
        if (bookmarked) {
            graphics.drawString(getFont(), "*", x + width - 9, y + 6,
                    ResearchArchiveLayer.COLOR_RED, false);
        }
        String title = research.getName().getString();
        title = getFont().plainSubstrByWidth(title, width - (bookmarked ? 25 : 17));
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
            tooltip.accept(research.getName());
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
        int listTop = HEADER_HEIGHT;
        int localY = (int) getMouseY();
        if (localY < listTop || localY >= getHeight()) {
            return null;
        }
        int index = (localY - listTop + state.typeListScroll(state.researchTypeFilter()))
                / RESEARCH_ROW_HEIGHT;
        List<Research> visible = visibleResearches();
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    private List<Research> visibleResearches() {
        String filter = state.researchTypeFilter();
        String query = state.searchQuery().toLowerCase(Locale.ROOT);
        String currentId = currentResearchId();
        List<Research> visible = new ArrayList<>();
        for (Research research : definitions) {
            if (!ResearchTypeIdNormalizer.ALL_TYPES.equals(filter)
                    && !ResearchTypeIdNormalizer.normalize(research.getCategory()).equals(filter)) {
                continue;
            }
            if (!query.isEmpty()
                    && !research.getId().toLowerCase(Locale.ROOT).contains(query)
                    && !research.getName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            visible.add(research);
        }
        visible.sort(Comparator
                .comparing((Research research) -> !state.bookmarkedResearchIds().contains(research.getId()))
                .thenComparing(research -> !research.getId().equals(currentId))
                .thenComparing(research -> research.getName().getString(), String.CASE_INSENSITIVE_ORDER));
        return visible;
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
        Research current = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
        return current == null ? null : current.getId();
    }
}
