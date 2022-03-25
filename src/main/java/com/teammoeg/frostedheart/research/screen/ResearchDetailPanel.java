package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public class ResearchDetailPanel extends Panel {
	Research r;
	Icon ci;
	public ResearchDetailPanel(Panel panel) {
		super(panel);
		this.setOnlyInteractWithWidgetsInside(true);
		this.setOnlyRenderWidgetsInside(true);
	}

	@Override
	public void addWidgets() {
		if(r==null)return;
		ci=ItemIcon.getItemIcon(r.getIcon());
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(r==null)return;
		super.draw(matrixStack,theme, x, y, w, h);
		theme.drawString(matrixStack,r.getName(), x+3, y+3);
		ci.draw(matrixStack, x+13, y+13, 32,32);
		
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		//theme.drawPanelBackground(matrixStack, x, y, w, h);
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}

}
