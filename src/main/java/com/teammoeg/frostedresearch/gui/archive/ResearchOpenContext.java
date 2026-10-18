/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedresearch.gui.archive;

import javax.annotation.Nullable;

/** Describes how the research archive was opened without granting any action permission. */
public record ResearchOpenContext(
        Mode mode,
        @Nullable String initialResearchId,
        ResearchWorkspaceState.ProjectTab initialProjectTab,
        @Nullable String initialClueNonce) {

    public ResearchOpenContext {
        if (mode == null) {
            mode = Mode.BROWSE;
        }
        if (initialProjectTab == null) {
            initialProjectTab = ResearchWorkspaceState.ProjectTab.DETAIL;
        }
        if (initialResearchId == null) {
            initialClueNonce = null;
        }
    }

    public static ResearchOpenContext browse() {
        return new ResearchOpenContext(Mode.BROWSE, null, ResearchWorkspaceState.ProjectTab.DETAIL, null);
    }

    public static ResearchOpenContext drawingDesk(@Nullable String initialResearchId) {
        return new ResearchOpenContext(
                Mode.DRAWING_DESK,
                initialResearchId,
                ResearchWorkspaceState.ProjectTab.DETAIL,
                null);
    }

    public enum Mode {
        BROWSE,
        DRAWING_DESK
    }
}
