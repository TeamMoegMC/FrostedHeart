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
