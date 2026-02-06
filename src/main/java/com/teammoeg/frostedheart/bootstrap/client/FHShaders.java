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

package com.teammoeg.frostedheart.bootstrap.client;

import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.client.shader.management.Shader;
import com.teammoeg.frostedheart.FHMain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FHShaders extends Shaders {
    private static Shader INFRARED_VIEW;

    public static Shader getInfraredView() {
        if (INFRARED_VIEW == null) {
            INFRARED_VIEW = load(Shader.ShaderType.FRAGMENT, FHMain.rl("infrared_view"));
            addReloadListener(() -> INFRARED_VIEW = load(Shader.ShaderType.FRAGMENT, FHMain.rl("infrared_view")));
        }
        return INFRARED_VIEW;
    }

}
