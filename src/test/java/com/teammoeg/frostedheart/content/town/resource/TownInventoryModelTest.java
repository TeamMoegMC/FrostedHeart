/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resource;

import com.teammoeg.frostedheart.content.town.resource.action.ResourceActionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownInventoryModelTest {
    @Test
    void attemptRejectsWholeBatchWhileMaximizePartiallyAccepts() {
        TownInventoryModel.Mutation attempt = TownInventoryModel.settle(
                5.0, 3.0, ResourceActionMode.ATTEMPT);
        TownInventoryModel.Mutation maximize = TownInventoryModel.settle(
                5.0, 3.0, ResourceActionMode.MAXIMIZE);

        assertEquals(0.0, attempt.modifiedAmount(), 1.0e-12);
        assertEquals(5.0, attempt.residualAmount(), 1.0e-12);
        assertFalse(attempt.fullyApplied());
        assertEquals(3.0, maximize.modifiedAmount(), 1.0e-12);
        assertEquals(2.0, maximize.residualAmount(), 1.0e-12);
        assertFalse(maximize.fullyApplied());
        assertTrue(TownInventoryModel.settle(
                3.0, 3.0, ResourceActionMode.ATTEMPT).fullyApplied());
    }
}
