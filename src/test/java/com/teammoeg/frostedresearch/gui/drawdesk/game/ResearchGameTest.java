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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.drawdesk.game;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ResearchGameTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void forceWinFinishesOnlyARunningBoard() {
        var game = new ResearchGame();
        assertFalse(game.forceWin(), "no game has started");
        game.init(GenerateInfo.all[0], new Random(1));
        game.setLvl(0);
        assertFalse(game.isFinished());
        assertTrue(game.get(ResearchGame.FINAL_CARD).isShow(), "every board starts with its final card at the centre");
        assertTrue(game.forceWin());
        assertTrue(game.isFinished());
        for (int x = 0; x < 9; x++)
            for (int y = 0; y < 9; y++)
                assertFalse(game.get(x, y).isShow());
        assertFalse(game.forceWin(), "a finished game is not won twice");
    }
}
