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

import com.teammoeg.chorda.client.cui.base.PanZoomViewport.Camera;
import com.teammoeg.frostedresearch.gui.archive.graph.ResearchTypeIdNormalizer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Client-only navigation and viewport state for the shared research archive.
 * It deliberately contains no research progress and is never serialized into team data.
 */
public final class ResearchWorkspaceState {
    public static final double MIN_ZOOM = 0.15D;
    public static final double MAX_ZOOM = 1.75D;

    private Surface surface;
    private String researchTypeFilter = ResearchTypeIdNormalizer.ALL_TYPES;
    private String searchQuery = "";
    private String selectedResearchId;
    private String selectedClueNonce;
    private boolean projectWorkspaceOpen;
    private ProjectTab projectTab = ProjectTab.DETAIL;
    private final Set<String> bookmarkedResearchIds = new LinkedHashSet<>();
    private boolean typeListExpanded = true;
    private final Map<String, Integer> typeListScrollByType = new HashMap<>();
    private final Map<String, Camera> cameraByResearchType = new HashMap<>();
    private DrawDeskFocusTarget drawDeskFocusTarget = DrawDeskFocusTarget.NONE;

    public ResearchWorkspaceState(ResearchOpenContext context) {
        Objects.requireNonNull(context, "context");
        this.surface = context.mode() == ResearchOpenContext.Mode.DRAWING_DESK
                ? Surface.DRAWING_DESK
                : Surface.RESEARCH_ARCHIVE;
        this.selectedResearchId = context.initialResearchId();
        this.selectedClueNonce = context.initialClueNonce();
        this.projectWorkspaceOpen = context.initialResearchId() != null;
        this.projectTab = context.initialProjectTab();
    }

    public Surface surface() {
        return surface;
    }

    public void setSurface(Surface surface) {
        this.surface = Objects.requireNonNull(surface, "surface");
    }

    public String researchTypeFilter() {
        return researchTypeFilter;
    }

    public void setResearchTypeFilter(@Nullable String researchTypeFilter) {
        this.researchTypeFilter = ResearchTypeIdNormalizer.normalize(researchTypeFilter);
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(@Nullable String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery.trim();
    }

    @Nullable
    public String selectedResearchId() {
        return selectedResearchId;
    }

    public void selectResearch(@Nullable String researchId) {
        if (!Objects.equals(selectedResearchId, researchId)) {
            selectedClueNonce = null;
        }
        selectedResearchId = researchId;
        if (researchId == null) {
            projectWorkspaceOpen = false;
        }
    }

    @Nullable
    public String selectedClueNonce() {
        return selectedClueNonce;
    }

    public void selectClue(@Nullable String clueNonce) {
        selectedClueNonce = clueNonce;
    }

    public boolean projectWorkspaceOpen() {
        return projectWorkspaceOpen;
    }

    public void setProjectWorkspaceOpen(boolean projectWorkspaceOpen) {
        this.projectWorkspaceOpen = projectWorkspaceOpen && selectedResearchId != null;
        if (!this.projectWorkspaceOpen) {
            selectedClueNonce = null;
        }
    }

    public ProjectTab projectTab() {
        return projectTab;
    }

    public void setProjectTab(ProjectTab projectTab) {
        this.projectTab = Objects.requireNonNull(projectTab, "projectTab");
    }

    public Set<String> bookmarkedResearchIds() {
        return Collections.unmodifiableSet(bookmarkedResearchIds);
    }

    public void setBookmarked(String researchId, boolean bookmarked) {
        if (bookmarked) {
            bookmarkedResearchIds.add(researchId);
        } else {
            bookmarkedResearchIds.remove(researchId);
        }
    }

    public boolean typeListExpanded() {
        return typeListExpanded;
    }

    public void setTypeListExpanded(boolean typeListExpanded) {
        this.typeListExpanded = typeListExpanded;
    }

    public int typeListScroll(String researchType) {
        return typeListScrollByType.getOrDefault(ResearchTypeIdNormalizer.normalize(researchType), 0);
    }

    public void setTypeListScroll(String researchType, int scroll) {
        typeListScrollByType.put(ResearchTypeIdNormalizer.normalize(researchType), Math.max(0, scroll));
    }

    public Camera camera(String researchType) {
        return cameraByResearchType.getOrDefault(ResearchTypeIdNormalizer.normalize(researchType), Camera.DEFAULT);
    }

    public void setCamera(String researchType, Camera camera) {
        cameraByResearchType.put(
                ResearchTypeIdNormalizer.normalize(researchType),
                Objects.requireNonNull(camera, "camera").clamped(MIN_ZOOM, MAX_ZOOM));
    }

    public DrawDeskFocusTarget consumeDrawDeskFocusTarget() {
        DrawDeskFocusTarget target = drawDeskFocusTarget;
        drawDeskFocusTarget = DrawDeskFocusTarget.NONE;
        return target;
    }

    public void setDrawDeskFocusTarget(DrawDeskFocusTarget drawDeskFocusTarget) {
        this.drawDeskFocusTarget = Objects.requireNonNull(drawDeskFocusTarget, "drawDeskFocusTarget");
    }

    /** Reconciles definition-backed state after a reload without resetting valid camera or list state. */
    public void retainDefinitions(
            Collection<String> researchIds,
            Collection<String> researchTypeIds,
            @Nullable String currentResearchId,
            @Nullable String firstVisibleResearchId) {
        Set<String> validResearchIds = new HashSet<>(researchIds);
        Set<String> validResearchTypes = new HashSet<>();
        validResearchTypes.add(ResearchTypeIdNormalizer.ALL_TYPES);
        for (String typeId : researchTypeIds) {
            validResearchTypes.add(ResearchTypeIdNormalizer.normalize(typeId));
        }

        if (!validResearchTypes.contains(researchTypeFilter)) {
            researchTypeFilter = ResearchTypeIdNormalizer.ALL_TYPES;
        }
        cameraByResearchType.keySet().retainAll(validResearchTypes);
        typeListScrollByType.keySet().retainAll(validResearchTypes);
        bookmarkedResearchIds.retainAll(validResearchIds);

        if (!validResearchIds.contains(selectedResearchId)) {
            if (validResearchIds.contains(currentResearchId)) {
                selectedResearchId = currentResearchId;
            } else if (validResearchIds.contains(firstVisibleResearchId)) {
                selectedResearchId = firstVisibleResearchId;
            } else {
                selectedResearchId = null;
            }
            selectedClueNonce = null;
            projectWorkspaceOpen = selectedResearchId != null && projectWorkspaceOpen;
        }
    }

    public void retainClues(Collection<String> clueNonces) {
        if (selectedClueNonce != null && !clueNonces.contains(selectedClueNonce)) {
            selectedClueNonce = null;
        }
    }

    public enum Surface {
        RESEARCH_ARCHIVE,
        DRAWING_DESK
    }

    public enum ProjectTab {
        DETAIL,
        THEORY,
        EXPERIMENT
    }

    public enum DrawDeskFocusTarget {
        NONE,
        ITEM_EXAMINE,
        THEORY_GAME
    }

}
