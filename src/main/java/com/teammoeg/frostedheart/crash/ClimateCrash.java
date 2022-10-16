/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.crash;

import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.ICrashCallable;

public class ClimateCrash implements ICrashCallable {
    public static ChunkPos Last;

    @Override
    public String call() throws Exception {
        return "last calculating climate chunk: " + Last;
    }

    @Override
    public String getLabel() {
        return "FHClimate";
    }

}
