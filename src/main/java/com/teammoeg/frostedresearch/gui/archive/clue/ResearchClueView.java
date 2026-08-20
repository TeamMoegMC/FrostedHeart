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
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/** Immutable, presentation-only snapshot of one clue row. */
public record ResearchClueView(
        String nonce,
        Component title,
        @Nullable Component description,
        @Nullable Component hint,
        boolean required,
        boolean completed,
        float contribution,
        ClueDestination destination,
        ResearchWorkspaceState.ProjectTab tab,
        int definitionOrder,
        boolean systemClue) {
}
