/*
 * Copyright (c) 2024 TeamMoeg
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
import com.teammoeg.frostedresearch.gui.ResearchGui;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.IScreenWrapper;
import net.minecraft.client.gui.screens.Screen;

public class ResearchUtils {
    public static void refreshResearchGui() {
        Screen cur = ClientUtils.getMc().screen;
        if (cur instanceof IScreenWrapper) {
            BaseScreen bs = ((IScreenWrapper) cur).getGui();
            if (bs instanceof ResearchGui) {
                bs.refreshWidgets();
            }
        }
    }
}
