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

import com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceState;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;

/** Assigns a clue to a presentation tab without changing its underlying type or data. */
public final class CluePresentationClassifier {
    private CluePresentationClassifier() {
    }

    public static ResearchWorkspaceState.ProjectTab classify(Clue clue) {
        return clue instanceof MinigameClue
                ? ResearchWorkspaceState.ProjectTab.THEORY
                : ResearchWorkspaceState.ProjectTab.DETAIL;
    }
}
