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
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskContainer;
import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Knowledge lives inside the CUI desk container; opening it does not replace or close the menu.
 */
public final class KnowledgeScreen {
    private KnowledgeScreen() {
    }

    public static void open(Screen parent, DrawDeskContainer menu) {
        if (parent instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen desk)
            desk.showKnowledge();
    }

    public static KnowledgeLayer current() {
        return Minecraft.getInstance().screen instanceof CUIScreen cui && cui.getPrimaryLayer() instanceof DrawDeskScreen desk ? desk.getKnowledgeLayer() : null;
    }
}
