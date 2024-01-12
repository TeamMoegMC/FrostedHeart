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

import se.mickelus.tetra.blocks.workbench.gui.WorkbenchStatsGui;
import se.mickelus.tetra.gui.stats.StatsHelper;
import se.mickelus.tetra.gui.stats.bar.GuiStatBarTool;
import se.mickelus.tetra.items.modular.impl.holo.gui.craft.HoloStatsGui;

public class TetraClient {
    public static void init() {
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.coreSpade));
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.geoHammer));
        WorkbenchStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.proPick));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.coreSpade));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.geoHammer));
        HoloStatsGui.addBar(new GuiStatBarTool(0, 0, StatsHelper.barLength, TetraCompat.proPick));
    }
}
