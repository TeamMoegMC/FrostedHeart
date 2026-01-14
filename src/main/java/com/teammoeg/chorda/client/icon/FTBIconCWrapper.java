package com.teammoeg.chorda.client.icon;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;

public class FTBIconCWrapper extends CIcon {
	private final Icon icon;

	public FTBIconCWrapper(Icon icon) {
		this.icon = icon;
	}

	@Override
	public void draw(GuiGraphics arg0, int arg1, int arg2, int arg3, int arg4) {
		try {
		icon.draw(arg0, arg1, arg2, arg3, arg4);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public boolean isEmpty() {
		return icon.isEmpty();
	}

}