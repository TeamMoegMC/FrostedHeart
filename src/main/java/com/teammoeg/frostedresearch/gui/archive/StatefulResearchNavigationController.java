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

import java.util.Objects;

/** Default state-machine implementation shared by the browse and drawing-desk wrappers. */
public final class StatefulResearchNavigationController implements ResearchNavigationController {
    private final ResearchOpenContext openContext;
    private final ResearchWorkspaceState state;
    private final Runnable closeScreen;

    public StatefulResearchNavigationController(
            ResearchOpenContext openContext,
            ResearchWorkspaceState state,
            Runnable closeScreen) {
        this.openContext = Objects.requireNonNull(openContext, "openContext");
        this.state = Objects.requireNonNull(state, "state");
        this.closeScreen = Objects.requireNonNull(closeScreen, "closeScreen");
    }

    @Override
    public void openResearch(String researchId) {
        state.setSurface(ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE);
        state.selectResearch(Objects.requireNonNull(researchId, "researchId"));
        state.setProjectWorkspaceOpen(true);
    }

    @Override
    public void openClue(String researchId, String clueNonce) {
        openResearch(researchId);
        state.selectClue(Objects.requireNonNull(clueNonce, "clueNonce"));
    }

    @Override
    public void goToDrawingDesk(ResearchWorkspaceState.DrawDeskFocusTarget target) {
        if (openContext.mode() != ResearchOpenContext.Mode.DRAWING_DESK) {
            return;
        }
        state.setDrawDeskFocusTarget(target);
        state.setSurface(ResearchWorkspaceState.Surface.DRAWING_DESK);
    }

    @Override
    public void returnToWorld() {
        closeScreen.run();
    }

    @Override
    public boolean back() {
        if (state.surface() == ResearchWorkspaceState.Surface.RESEARCH_ARCHIVE) {
            if (state.projectWorkspaceOpen()) {
                state.setProjectWorkspaceOpen(false);
                return true;
            }
            if (openContext.mode() == ResearchOpenContext.Mode.DRAWING_DESK) {
                state.setSurface(ResearchWorkspaceState.Surface.DRAWING_DESK);
                return true;
            }
        }
        return false;
    }
}
