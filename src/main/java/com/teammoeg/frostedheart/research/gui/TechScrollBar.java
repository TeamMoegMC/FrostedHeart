package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public class TechScrollBar extends PanelScrollBar {
	public TechScrollBar(Panel parent, Panel panel) {
		super(parent, panel);
	}

	public TechScrollBar(Panel parent, Plane plane, Panel p) {
		super(parent, plane, p);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.setupDrawing();
		TechIcons.SLIDER_FRAME.draw(matrixStack, x, y, w, h);
	}

	// Have to do this hack since FTBL fields are private.
	private static final Theme dtheme = new Theme() {
		@Override
		public void drawScrollBar(MatrixStack matrixStack, int x, int y, int w, int h, WidgetType type,
				boolean vertical) {
			GuiHelper.setupDrawing();
			TechIcons.drawTexturedRect(matrixStack, x, y, w, h, type != WidgetType.MOUSE_OVER);
		}
	};

	@Override
	public void drawScrollBar(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

		super.drawScrollBar(matrixStack, dtheme, x + 1, y + 1, w - 2, h - 2);
	}

	@Override
	public boolean canMouseScroll() {
		return super.canMouseScroll()&&panel.isEnabled();
	}

}
