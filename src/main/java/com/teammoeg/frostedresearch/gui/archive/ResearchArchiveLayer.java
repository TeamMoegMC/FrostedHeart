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
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Responsive CUI surface for browsing research definitions and synchronized progress. */
public final class ResearchArchiveLayer extends UILayer {
    static final int COLOR_INK = 0xFF201E1A;
    static final int COLOR_MUTED_INK = 0xFF6E675B;
    static final int COLOR_PAPER = 0xFFF0E7CF;
    static final int COLOR_PAPER_DARK = 0xFFD4C7A8;
    static final int COLOR_PANEL = 0xFFE5D9BC;
    static final int COLOR_PANEL_DARK = 0xFFC2B38F;
    static final int COLOR_GRAPH = 0xFF292C2B;
    static final int COLOR_GRAPH_GRID = 0xFF3A3E3A;
    static final int COLOR_TEAL = 0xFF477A73;
    static final int COLOR_RED = 0xFF9A493D;
    static final int COLOR_GOLD = 0xFFB18A42;
    static final int HEADER_HEIGHT = 31;
    static final int HEADER_TITLE_WIDTH = 100;
    static final int HEADER_ACTION_WIDTH = 76;
    static final int FIELD_TABS_Y = 2;
    static final int CONTENT_TOP = HEADER_HEIGHT + 4;
    private static final Component ARCHIVE_TITLE =
            Component.translatable("gui.frostedresearch.archive.title");
    private static final Component DRAWING_DESK =
            Component.translatable("gui.frostedresearch.archive.drawing_desk");

    private final ResearchOpenContext openContext;
    private final ResearchWorkspaceState state;
    private final ResearchNavigationController navigation;
    private final Runnable surfaceChanged;
    private final ResearchArchiveViewCache viewCache;
    private final ResearchFieldTabBar fieldTabs;
    private final TextBox searchBox;
    private final ResearchTypeListPanel typeList;
    private final ResearchGraphViewport graphViewport;
    private final ResearchProjectSummaryPanel projectSummary;
    private final ResearchProjectWorkspace projectWorkspace;
    private long definitionRevision;
    private boolean definitionsInitialized;
    private boolean definitionEditorMode = FHResearch.editor;
    private Set<String> visibleDefinitionIds = Set.of();
    @Nullable
    private ArchiveLayoutKey layoutKey;

    public ResearchArchiveLayer(
            UIElement parent,
            ResearchOpenContext openContext,
            ResearchWorkspaceState state,
            ResearchNavigationController navigation,
            Runnable surfaceChanged) {
        super(parent);
        this.openContext = Objects.requireNonNull(openContext, "openContext");
        this.state = Objects.requireNonNull(state, "state");
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.surfaceChanged = Objects.requireNonNull(surfaceChanged, "surfaceChanged");
        this.viewCache = new ResearchArchiveViewCache();
        this.fieldTabs = new ResearchFieldTabBar(this, state);
        this.searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                ResearchArchiveLayer.this.state.setSearchQuery(getText());
                typeList.onFilterChanged();
                graphViewport.onSearchChanged();
            }
        };
        this.searchBox.setMaxLength(96);
        this.searchBox.ghostText = Component.translatable("gui.frostedresearch.archive.search").getString();
        this.typeList = new ResearchTypeListPanel(this, state, viewCache, this::onNavigationChanged);
        this.graphViewport = new ResearchGraphViewport(
                this, state, viewCache, navigation, this::onNavigationChanged);
        this.projectSummary = new ResearchProjectSummaryPanel(
                this, state, viewCache, navigation, this::onNavigationChanged);
        this.projectWorkspace = new ResearchProjectWorkspace(
                this, openContext, state, viewCache, navigation, this::onNavigationChanged);
        this.searchBox.setText(state.searchQuery(), false);
        setScissorEnabled(false);
    }

    @Override
    public void addUIElements() {
        add(fieldTabs);
        add(searchBox);
        add(typeList);
        add(graphViewport);
        add(projectSummary);
        add(projectWorkspace);
        if (!definitionsInitialized) {
            rebuildDefinitions();
        }
    }

    @Override
    public void alignWidgets() {
        resizeArchive(getWidth(), getHeight());
    }

    public void resizeArchive(int width, int height) {
        int safeWidth = Math.max(280, width);
        int safeHeight = Math.max(188, height);
        boolean workspaceOpen = state.projectWorkspaceOpen();
        if (layoutKey != null && layoutKey.matches(safeWidth, safeHeight, workspaceOpen)) {
            return;
        }
        setSize(safeWidth, safeHeight);
        fieldTabs.setPosAndSize(
                HEADER_TITLE_WIDTH,
                FIELD_TABS_Y,
                Math.max(1, safeWidth - HEADER_TITLE_WIDTH - HEADER_ACTION_WIDTH),
                HEADER_HEIGHT - FIELD_TABS_Y - 1);

        int contentTop = CONTENT_TOP;
        int contentHeight = safeHeight - contentTop - 6;
        int listWidth = safeWidth >= 620 ? 142 : safeWidth < 340 ? 72 : 92;
        searchBox.setPosAndSize(6, contentTop, listWidth, 17);
        typeList.setPosAndSize(6, contentTop + 21, listWidth, Math.max(36, contentHeight - 21));

        int graphX = typeList.getX() + typeList.getWidth() + 5;
        int summaryWidth = safeWidth >= 620 ? 176 : safeWidth < 340 ? 84 : 112;
        int graphWidth = Math.max(82, safeWidth - graphX - summaryWidth - 11);
        graphViewport.resizeViewport(graphX, contentTop, graphWidth, contentHeight);
        projectSummary.setPosAndSize(
                graphViewport.getX() + graphViewport.getWidth() + 5,
                contentTop,
                summaryWidth,
                contentHeight);

        if (workspaceOpen) {
            int modalWidth = Math.min(302, safeWidth - 16);
            int modalHeight = Math.min(170, safeHeight - 16);
            projectWorkspace.setPosAndSize(
                    (safeWidth - modalWidth) / 2,
                    (safeHeight - modalHeight) / 2,
                    modalWidth,
                    modalHeight);
        }
        fieldTabs.setEnabled(!workspaceOpen);
        searchBox.setEnabled(!workspaceOpen);
        typeList.setEnabled(!workspaceOpen);
        graphViewport.setEnabled(!workspaceOpen);
        graphViewport.setVisible(!workspaceOpen);
        projectSummary.setEnabled(!workspaceOpen);
        projectWorkspace.setVisible(workspaceOpen);
        projectWorkspace.setEnabled(workspaceOpen);
        layoutKey = new ArchiveLayoutKey(safeWidth, safeHeight, workspaceOpen);
    }

    public void onResearchDefinitionsChanged() {
        rebuildDefinitions();
    }

    public void onResearchDataReplaced() {
        rebuildDefinitions();
    }

    public void onResearchProgressChanged(@Nullable String researchId) {
        if (!visibleDefinitionIds.equals(currentVisibleDefinitionIds())) {
            rebuildDefinitions();
            return;
        }
        viewCache.refreshStates();
        typeList.onProgressChanged(researchId);
        graphViewport.onProgressChanged(researchId);
        projectWorkspace.onProgressChanged(researchId);
    }

    public void onActiveResearchChanged(@Nullable String researchId) {
        viewCache.refreshStates();
        typeList.onActiveResearchChanged();
        graphViewport.onProgressChanged(researchId);
        projectWorkspace.onActiveResearchChanged(researchId);
    }

    public void onClueProgressChanged(String researchId, String clueNonce) {
        if (!visibleDefinitionIds.equals(currentVisibleDefinitionIds())) {
            rebuildDefinitions();
            return;
        }
        viewCache.refreshStates();
        graphViewport.onProgressChanged(researchId);
        projectWorkspace.onClueProgressChanged(researchId, clueNonce);
    }

    void setResearchTypeFilter(String researchTypeId) {
        state.setResearchTypeFilter(researchTypeId);
        typeList.onFilterChanged();
        graphViewport.onResearchTypeChanged();
    }

    boolean refreshEditorModeIfNeeded() {
        if (!definitionsInitialized || definitionEditorMode == FHResearch.editor) {
            return false;
        }
        rebuildDefinitions();
        return true;
    }

    private void rebuildDefinitions() {
        boolean editorMode = FHResearch.editor;
        List<Research> definitions = FHResearch.getAllResearch().stream()
                .filter(research -> isDefinitionVisible(research, editorMode))
                .toList();
        visibleDefinitionIds = definitions.stream()
                .map(Research::getId)
                .collect(Collectors.toUnmodifiableSet());
        String currentResearchId = currentResearchId();
        String firstVisibleResearchId = definitions.stream()
                .map(Research::getId)
                .sorted()
                .findFirst()
                .orElse(null);
        state.retainDefinitions(
                definitions.stream().map(Research::getId).toList(),
                definitions.stream()
                        .map(research -> ResearchTypeIdNormalizer.normalize(research.getCategory()))
                        .distinct()
                        .toList(),
                currentResearchId,
                firstVisibleResearchId);
        viewCache.setDefinitions(definitions);
        typeList.setDefinitions(definitions);
        graphViewport.setDefinitions(definitions, ++definitionRevision);
        projectSummary.setDefinitions(definitions);
        projectWorkspace.setDefinitions(definitions);
        definitionEditorMode = editorMode;
        definitionsInitialized = true;
        resizeArchive(getWidth(), getHeight());
    }

    @Nullable
    private String currentResearchId() {
        Research current = ClientResearchDataAPI.getData().get().getCurrentResearchValue();
        return current == null ? null : current.getId();
    }

    private void onNavigationChanged() {
        resizeArchive(getWidth(), getHeight());
        surfaceChanged.run();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (state.projectWorkspaceOpen() && !projectWorkspace.isMouseOver()) {
            state.setProjectWorkspaceOpen(false);
            resizeArchive(getWidth(), getHeight());
            return true;
        }
        if (super.onMousePressed(button)) {
            return true;
        }
        if (!isMouseOver() || button != MouseButton.LEFT) {
            return false;
        }
        int localX = (int) getMouseX();
        int localY = (int) getMouseY();
        if (localY <= HEADER_HEIGHT && localX >= getWidth() - HEADER_ACTION_WIDTH) {
            navigation.goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget.NONE);
            onNavigationChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (state.projectWorkspaceOpen() && !projectWorkspace.isMouseOver()) {
            return true;
        }
        return super.onMouseScrolled(scroll);
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        if (state.projectWorkspaceOpen()) {
            if (projectWorkspace.hasTooltip()) {
                projectWorkspace.getTooltip(tooltip);
            }
            return;
        }
        super.getTooltip(tooltip);
    }

    @Override
    public Cursor getCursor() {
        if (state.projectWorkspaceOpen()) {
            return projectWorkspace.isMouseOver() ? projectWorkspace.getCursor() : null;
        }
        return super.getCursor();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        refreshEditorModeIfNeeded();
        if (viewCache.refreshLanguageIfNeeded()) {
            searchBox.ghostText = Component.translatable("gui.frostedresearch.archive.search").getString();
            typeList.onPresentationChanged();
            graphViewport.onPresentationChanged();
            projectSummary.onPresentationChanged();
        }
        graphics.fill(x, y, x + width, y + height, COLOR_INK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEADER_HEIGHT - 1, COLOR_PAPER);
        graphics.fill(x + 1, y + HEADER_HEIGHT - 1, x + width - 1, y + HEADER_HEIGHT, COLOR_RED);
        graphics.drawString(getFont(), ARCHIVE_TITLE, x + 10, y + 10, COLOR_INK, false);
        int actionX = x + width - HEADER_ACTION_WIDTH;
        boolean hovered = getMouseY() <= HEADER_HEIGHT
                && getMouseX() >= width - HEADER_ACTION_WIDTH;
        graphics.fill(actionX, y + 5, x + width - 6, y + 25, hovered ? COLOR_RED : COLOR_TEAL);
        int textX = actionX + Math.max(4, (70 - getFont().width(DRAWING_DESK)) / 2);
        graphics.drawString(getFont(), DRAWING_DESK, textX, y + 11, 0xFFF8F1DE, false);
    }

    static boolean canReveal(Research research) {
        return FHResearch.editor || research.isShowable() || research.isUnlocked() || research.isCompleted();
    }

    static boolean isDefinitionVisible(Research research) {
        return isDefinitionVisible(research, FHResearch.editor);
    }

    private static boolean isDefinitionVisible(Research research, boolean editor) {
        return definitionVisible(
                editor,
                research.isHidden(),
                research.isShowable(),
                research.isUnlocked(),
                research.isCompleted());
    }

    static boolean definitionVisible(
            boolean editor, boolean hidden, boolean showable, boolean unlocked, boolean completed) {
        return editor || (!hidden && (showable || unlocked || completed));
    }

    private Set<String> currentVisibleDefinitionIds() {
        boolean editorMode = FHResearch.editor;
        return FHResearch.getAllResearch().stream()
                .filter(research -> isDefinitionVisible(research, editorMode))
                .map(Research::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record ArchiveLayoutKey(int width, int height, boolean workspaceOpen) {
        private boolean matches(int currentWidth, int currentHeight, boolean currentWorkspaceOpen) {
            return width == currentWidth
                    && height == currentHeight
                    && workspaceOpen == currentWorkspaceOpen;
        }
    }
}
