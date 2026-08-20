/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.gui.archive.clue;

import com.teammoeg.frostedresearch.gui.archive.ResearchOpenContext;
import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Builds display rows from existing clue definitions and synchronized progress. */
public final class ResearchClueViewFactory {
    public static final String EXPERIMENT_POINTS_NONCE = "$experiment_points";

    private ResearchClueViewFactory() {
    }

    public static List<ResearchClueView> create(
            Research research,
            ResearchData data,
            Context context) {
        Objects.requireNonNull(research, "research");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(context, "context");

        List<ResearchClueView> views = new ArrayList<>(research.getClues().size() + 1);
        int definitionOrder = 0;
        for (Clue clue : research.getClues()) {
            boolean completed = data.isClueTriggered(clue);
            boolean currentResearch = research.getId().equals(context.currentResearchId());
            boolean currentTheoryClue = clue.getNonce().equals(context.currentTheoryClueNonce());
            Component title = clue.getName(research);
            if (title == null) {
                title = Component.translatable("gui.frostedresearch.research.clue.unknown");
            }
            views.add(new ResearchClueView(
                    clue.getNonce(),
                    title,
                    clue.getDescription(research),
                    clue.getHint(research),
                    clue.isRequired(),
                    completed,
                    clue.getResearchContribution(),
                    ClueDestinationResolver.resolve(
                            clue,
                            completed,
                            new ClueDestinationResolver.Context(
                                    context.openMode(),
                                    currentResearch,
                                    currentTheoryClue)),
                    CluePresentationClassifier.classify(clue),
                    definitionOrder++,
                    false));
        }

        long requiredPoints = research.getRequiredPoints();
        long committedPoints = data.getTotalCommitted(research);
        boolean pointsComplete = committedPoints >= requiredPoints;
        views.add(new ResearchClueView(
                EXPERIMENT_POINTS_NONCE,
                Component.translatable(
                        "gui.frostedresearch.research.clue.experiment_points",
                        committedPoints,
                        requiredPoints),
                Component.translatable("gui.frostedresearch.research.clue.experiment_points.desc"),
                null,
                true,
                pointsComplete,
                0.0F,
                ClueDestination.NONE,
                ResearchWorkspaceState.ProjectTab.DETAIL,
                definitionOrder,
                true));

        views.sort(Comparator
                .comparingInt(ResearchClueViewFactory::sortGroup)
                .thenComparingInt(ResearchClueView::definitionOrder));
        return List.copyOf(views);
    }

    private static int sortGroup(ResearchClueView view) {
        if (view.completed()) {
            return 2;
        }
        return view.required() ? 0 : 1;
    }

    public record Context(
            ResearchOpenContext.Mode openMode,
            @Nullable String currentResearchId,
            @Nullable String currentTheoryClueNonce) {
    }
}
