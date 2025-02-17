package com.teammoeg.chorda.client.cui;


import com.teammoeg.chorda.client.ClientUtils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ScreenLayerHolder extends Screen implements LayerHolder,UIElementBase {
	Focusable lastFocused;
	public ScreenLayerHolder(Component pTitle) {
		super(pTitle);
	}
	int leftPos;
	int topPos;
	int imageWidth;
	int imageHeight;
	int mouseX;
	int mouseY;
	@Override
	protected void init() {
		super.init();
	      this.leftPos = (this.width - this.imageWidth) / 2;
	      this.topPos = (this.height - this.imageHeight) / 2;
	}

	@Override
	public void focusOn(Focusable elm) {
		if(lastFocused!=null) {
			lastFocused.setFocused(false);
		}
		if(elm!=null) {
			elm.setFocused(true);
			lastFocused=elm;
		}
	}

	@Override
	public Font getFont() {
		return ClientUtils.mc().font;
	}

	@Override
	public int getErrorColor() {
		return 0xa92b0d;
	}

	@Override
	public int getHighlightColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int getFrameColor() {
		return -6250336;
	}
	@Override
	public int getFontColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int getButtonShadowColor() {
		return -8355712;
	}

	@Override
	public int getBackgroundColor() {
		return -16777216;
	}
	@Override
	public int getButtonFaceColor() {
		return -4144960;
	}

	@Override
	public LayerHolder getLayerHolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getX() {
		return leftPos;
	}

	@Override
	public int getY() {
		return topPos;
	}

	@Override
	public boolean isMouseOver() {
		return false;
	}

	@Override
	public int getMouseX() {
		return mouseX;
	}

	@Override
	public int getMouseY() {
		return mouseY;
	}

}
