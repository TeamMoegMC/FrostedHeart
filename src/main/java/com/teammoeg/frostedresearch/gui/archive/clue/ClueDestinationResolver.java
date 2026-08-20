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
import com.teammoeg.frostedresearch.research.clues.AdvancementClue;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.clues.CustomClue;
import com.teammoeg.frostedresearch.research.clues.ItemClue;
import com.teammoeg.frostedresearch.research.clues.KillClue;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;
import com.teammoeg.frostedresearch.research.clues.TickListenerClue;

/** Maps existing clue types to client navigation targets without invoking research behavior. */
public final class ClueDestinationResolver {
    private ClueDestinationResolver() {
    }

    public static ClueDestination resolve(Clue clue, boolean completed, Context context) {
        if (completed) {
            return ClueDestination.NONE;
        }
        if (clue instanceof ItemClue) {
            return context.openMode() == ResearchOpenContext.Mode.DRAWING_DESK
                    ? ClueDestination.ITEM_EXAMINE
                    : ClueDestination.DRAWING_DESK_REQUIRED;
        }
        if (clue instanceof MinigameClue) {
            if (context.openMode() != ResearchOpenContext.Mode.DRAWING_DESK) {
                return ClueDestination.DRAWING_DESK_REQUIRED;
            }
            if (!context.currentResearch()) {
                return ClueDestination.START_RESEARCH_REQUIRED;
            }
            return context.currentTheoryClue()
                    ? ClueDestination.THEORY_GAME
                    : ClueDestination.PREVIOUS_THEORY_REQUIRED;
        }
        if (clue instanceof KillClue || clue instanceof AdvancementClue || clue instanceof TickListenerClue) {
            return ClueDestination.WORLD;
        }
        if (clue instanceof CustomClue) {
            return ClueDestination.DETAILS;
        }
        return ClueDestination.DETAILS;
    }

    public record Context(
            ResearchOpenContext.Mode openMode,
            boolean currentResearch,
            boolean currentTheoryClue) {
    }
}
