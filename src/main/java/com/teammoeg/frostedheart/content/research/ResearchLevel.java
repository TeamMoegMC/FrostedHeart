/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import com.teammoeg.frostedheart.util.TranslateUtils;

public enum ResearchLevel {
    DRAWING_DESK("drawing_desk"),
    MAPPING_MACHINE("mapping_machine"),
    MECHANICAL_CALCULATOR("mechanical_calculator"),
    DIFFERENTIAL_ENGINE("differential_engine"),
    COMPUTING_MATRIX("computing_matrix");

    final ResourceLocation icon;
    final Component name;

    ResearchLevel(String levelName) {
        icon = FHMain.rl("textures/gui/research/level/" + levelName);
        name = TranslateUtils.translateResearchLevel(levelName);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public Component getName() {
        return name;
    }
}
