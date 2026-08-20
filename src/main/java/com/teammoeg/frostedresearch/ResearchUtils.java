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

package com.teammoeg.frostedresearch;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreen;
import com.teammoeg.frostedresearch.gui.ResearchGui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;

public class ResearchUtils {
    public static void refreshResearchGui() {
        Screen cur = ClientUtils.getMc().screen;
        if (cur instanceof CUIScreen cs && cs.getPrimaryLayer() instanceof ResearchGui) {
            cs.getPrimaryLayer().refreshElements();
        }
    }

    public static void notifyResearchDefinitionsChanged() {
        ResearchGui gui = getOpenResearchGui();
        if (gui != null) {
            gui.onResearchDefinitionsChanged();
        }
    }

    public static void notifyResearchProgressChanged(String researchId) {
        ResearchGui gui = getOpenResearchGui();
        if (gui != null) {
            gui.onResearchProgressChanged(researchId);
        }
    }

    public static void notifyActiveResearchChanged(@Nullable String researchId) {
        ResearchGui gui = getOpenResearchGui();
        if (gui != null) {
            gui.onActiveResearchChanged(researchId);
        }
    }

    public static void notifyClueProgressChanged(String researchId, String clueNonce) {
        ResearchGui gui = getOpenResearchGui();
        if (gui != null) {
            gui.onClueProgressChanged(researchId, clueNonce);
        }
    }

    private static ResearchGui getOpenResearchGui() {
        Screen cur = ClientUtils.getMc().screen;
        if (cur instanceof CUIScreen cs) {
            if (cs.getPrimaryLayer() instanceof ResearchGui gui) {
                return gui;
            }
        }
        return null;
    }
}
