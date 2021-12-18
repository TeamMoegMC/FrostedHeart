package com.teammoeg.frostedheart.client.util;

import net.minecraft.client.gui.IGuiEventListener;

public class GuiClickedEvent implements IGuiEventListener {
	int x1;
	int y1;
	int x2;
	int y2;
	Runnable call;
	public GuiClickedEvent(int x1, int y1, int x2, int y2, Runnable call) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.call = call;
	}
	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if(x1<=mx&&mx<=x2&&y1<=my&&my<=y2) {
			call.run();
			return true;
		}
		return false;
	}

}
