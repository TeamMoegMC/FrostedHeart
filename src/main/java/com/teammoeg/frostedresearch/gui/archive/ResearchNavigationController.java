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

/** Pure client navigation boundary. Implementations must not send research packets. */
public interface ResearchNavigationController {
    void openResearch(String researchId);

    void openClue(String researchId, String clueNonce);

    void goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget target);

    void returnToWorld();

    /**
     * Navigates one level back.
     *
     * @return {@code true} when navigation consumed the action; {@code false} when the screen should close
     */
    boolean back();
}
