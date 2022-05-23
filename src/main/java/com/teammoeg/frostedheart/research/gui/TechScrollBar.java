package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public class TechScrollBar extends PanelScrollBar {
	public static class DDTheme extends Theme {

		public DDTheme() {
		}

		@Override
		public void drawScrollBar(MatrixStack matrixStack, int x, int y, int w, int h, WidgetType type, boolean vertical) {
			DrawDeskIcons.drawTexturedRect(matrixStack, x, y, w, h,type!=WidgetType.MOUSE_OVER);
		}

	}
	public TechScrollBar(Panel parent, Panel panel) {
		super(parent, panel);
	}

	public TechScrollBar(Panel parent, Plane plane, Panel p) {
		super(parent, plane, p);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		DrawDeskIcons.SLIDER_FRAME.draw(matrixStack, x, y, w, h);
	}
	private static final DDTheme dtheme=new DDTheme();
	@Override
	public void drawScrollBar(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawScrollBar(matrixStack,dtheme,x+1, y+1, w-2, h-2);
	}

}
