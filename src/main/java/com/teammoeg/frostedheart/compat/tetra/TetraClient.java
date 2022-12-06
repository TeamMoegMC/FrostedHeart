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
