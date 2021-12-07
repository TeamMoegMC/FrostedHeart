package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;

public class Line extends Widget {
	//diagonal direction
	private boolean xdirection=true;
	private boolean ydirection=true;
	//ensure w and h is positive
	@Override
	public void setHeight(int v) {
		if(v<0) {
			super.setY(posY+v);
			v=-v;
			xdirection=false;
		}else xdirection=true;
		super.setHeight(v);
	}
	@Override
	public void setWidth(int v) {
		if(v<0) {
			super.setX(posX+v);
			v=-v;
			ydirection=false;
		}else ydirection=true;
		super.setWidth(v);
	}
	@Override
	public void setX(int v) {
		super.setX(v);
	}
	@Override
	public void setY(int v) {
		super.setY(v);
	}
	public Line(Panel p) {
		super(p);
	}
	public int color;
	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if(xdirection^ydirection)
			FHGuiHelper.drawLine(matrixStack, color, x+w, y, x, y+h);
		else
			FHGuiHelper.drawLine(matrixStack, color, x, y, x+w, y+h);
		
		//super.draw(matrixStack, theme, x, y, w, h);
	}

}
