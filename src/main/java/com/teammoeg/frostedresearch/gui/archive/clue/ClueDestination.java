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

/** Presentation-only destination for a clue row. None of these values completes a clue. */
public enum ClueDestination {
    NONE,
    ITEM_EXAMINE,
    THEORY_GAME,
    WORLD,
    DETAILS,
    DRAWING_DESK_REQUIRED,
    START_RESEARCH_REQUIRED,
    PREVIOUS_THEORY_REQUIRED
}
