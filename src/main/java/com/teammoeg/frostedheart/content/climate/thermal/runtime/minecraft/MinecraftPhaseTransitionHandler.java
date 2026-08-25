/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Narrow integration point for modded phase transitions using CUSTOM actions. */
@FunctionalInterface
public interface MinecraftPhaseTransitionHandler {
    enum Outcome {
        APPLIED,
        REJECTED,
        RETRY
    }

    Outcome apply(
            ServerLevel level,
            BlockPos position,
            BlockState currentState,
            MaterialBoundaryRegistry.Profile profile
    );

    static MinecraftPhaseTransitionHandler rejectCustomActions() {
        return (level, position, currentState, profile) -> Outcome.REJECTED;
    }
}
