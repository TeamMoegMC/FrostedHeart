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

import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Client-only derived presentation and synchronized-state snapshot for archive widgets. */
final class ResearchArchiveViewCache {
    private List<Research> definitions = List.of();
    private Map<String, View> viewsById = Map.of();
    private Language language = Language.getInstance();
    private long presentationRevision;
    private long stateRevision;
    @Nullable
    private String activeResearchId;

    void setDefinitions(List<Research> definitions) {
        this.definitions = List.copyOf(definitions);
        Map<String, View> views = new LinkedHashMap<>();
        for (Research research : definitions) {
            views.put(research.getId(), new View(research));
        }
        viewsById = Map.copyOf(views);
        language = Language.getInstance();
        rebuildPresentations();
        refreshStates();
    }

    boolean refreshLanguageIfNeeded() {
        Language current = Language.getInstance();
        if (current == language) {
            return false;
        }
        language = current;
        rebuildPresentations();
        return true;
    }

    void refreshStates() {
        TeamResearchData teamData = ClientResearchDataAPI.getData().get();
        Research activeResearch = teamData.getCurrentResearchValue();
        activeResearchId = activeResearch == null ? null : activeResearch.getId();

        for (View view : viewsById.values()) {
            ResearchData data = teamData.getData(view.research);
            view.completed = data.isCompleted();
            float progress = data.getProgress(view.research);
            view.progress = Float.isFinite(progress)
                    ? Math.max(0.0F, Math.min(1.0F, progress))
                    : 0.0F;
            view.active = view.research.getId().equals(activeResearchId);
        }
        for (View view : viewsById.values()) {
            view.unlocked = parentsCompleted(view.research, teamData);
        }
        stateRevision++;
    }

    private boolean parentsCompleted(Research research, TeamResearchData teamData) {
        for (String parentId : research.getParentIds()) {
            View parentView = viewsById.get(parentId);
            if (parentView != null) {
                if (!parentView.completed) {
                    return false;
                }
                continue;
            }
            Research parent = FHResearch.getResearch(parentId);
            if (parent != null && !teamData.getData(parent).isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void rebuildPresentations() {
        for (View view : viewsById.values()) {
            Component name = view.research.getName();
            String title = name.getString();
            Component categoryName = view.research.getCategory().getName();
            view.name = name;
            view.title = title;
            view.lowercaseSearchText = (view.research.getId() + '\n' + title).toLowerCase(Locale.ROOT);
            view.categoryName = categoryName;
            view.categoryTitle = categoryName.getString();
        }
        presentationRevision++;
    }

    @Nullable
    View view(String researchId) {
        return viewsById.get(researchId);
    }

    List<Research> definitions() {
        return definitions;
    }

    @Nullable
    String activeResearchId() {
        return activeResearchId;
    }

    long presentationRevision() {
        return presentationRevision;
    }

    long stateRevision() {
        return stateRevision;
    }

    static final class View {
        private final Research research;
        private Component name = Component.empty();
        private String title = "";
        private String lowercaseSearchText = "";
        private Component categoryName = Component.empty();
        private String categoryTitle = "";
        private boolean completed;
        private boolean active;
        private boolean unlocked;
        private float progress;

        private View(Research research) {
            this.research = research;
        }

        Research research() {
            return research;
        }

        Component name() {
            return name;
        }

        String title() {
            return title;
        }

        String lowercaseSearchText() {
            return lowercaseSearchText;
        }

        Component categoryName() {
            return categoryName;
        }

        String categoryTitle() {
            return categoryTitle;
        }

        boolean completed() {
            return completed;
        }

        boolean active() {
            return active;
        }

        boolean unlocked() {
            return unlocked;
        }

        float progress() {
            return progress;
        }
    }
}
