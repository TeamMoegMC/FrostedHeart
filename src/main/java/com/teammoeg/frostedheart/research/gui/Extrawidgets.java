package com.teammoeg.frostedheart.research.gui;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;

public class Extrawidgets {
	private static final ImageIcon CREATIVE_INVENTORY_TABS = (ImageIcon) Icon.getIcon("textures/gui/container/creative_inventory/tabs.png");
	public static final Icon TAB_V_UNSELECTED = CREATIVE_INVENTORY_TABS.withUV(0, 0, 28, 32, 256, 256);
	public static final Icon TAB_V_SELECTED = CREATIVE_INVENTORY_TABS.withUV(0, 32, 28, 32, 256, 256);
}
