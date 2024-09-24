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

package com.teammoeg.frostedheart.compat.tetra;

import net.minecraftforge.common.ToolAction;

public class TetraCompat {
    public static ToolAction coreSpade = ToolAction.get("core_spade");
    public static ToolAction proPick = ToolAction.get("prospector_pick");
    public static ToolAction geoHammer = ToolAction.get("geo_hammer");

    public static void init() {
    }
}
