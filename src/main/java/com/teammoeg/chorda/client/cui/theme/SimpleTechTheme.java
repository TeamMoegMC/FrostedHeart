package com.teammoeg.chorda.client.cui.theme;

import net.minecraft.client.gui.GuiGraphics;

public class SimpleTechTheme implements Theme{
	public static final SimpleTechTheme INSTANCE=new SimpleTechTheme();
	protected SimpleTechTheme() {
	}

	@Override
	public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
		int color=0xffe4eff0;
		if(!enabled)
			color=0xFF002439;
		drawRect(graphics,x,y,w-1,h-1, color);
		if(isHighlight) {
			graphics.fill(x+1, y+1, x+w-1, y+h-1, 0x33FFFFFF);
		}
	}

	@Override
	public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
		drawRect(graphics,x+1,y+1,w-3,h-3,0xffe4eff0);
	}

	@Override
	public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {
		drawRect(graphics,x,y,w,h, 0xffe4eff0);
		
	}

	@Override
	public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {
		graphics.fill(x+1, y+1, x + w - 1, y + h - 1,0xffe4eff0);
	}

	@Override
	public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {
		drawRect(graphics,x,y,w,h, 0xffe4eff0);
	}

	@Override
	public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {
		drawRect(graphics,x,y,w,h, 0xffe4eff0);
	}
	protected void drawRect(GuiGraphics graphics, int x, int y, int w, int h,int color) {
		graphics.hLine(x, x+w, y, color);
		graphics.hLine(x, x+w, y+h, color);
		graphics.vLine(x, y, y+h, color);
		graphics.vLine(x+w, y, y+h, color);
	}
	@Override
	public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x+w, y+h, UIBGColor());
		drawRect(graphics,x,y,w,h, UIBGBorderColor());
	}
	@Override
	public boolean isUITextShadow() {
		return false;
	}
	
	@Override
	public boolean isButtonTextShadow() {
		return false;
	}
	
	@Override
	public int UITextColor() {
		return 0xFFe4eff0;
	}

	@Override
	public int UIAltTextColor() {
		return 0xFFe4eff0;
	}

	@Override
	public int UIBGColor() {
		return 0xaa005066;
	}

	@Override
	public int UIBGBorderColor() {
		return 0xffe4eff0;
	}

	@Override
	public int errorColor() {
		return 0xFFAA9999;
	}

	@Override
	public int successColor() {
		return 0xFF99AA99;
	}


	@Override
	public int buttonTextColor() {
		return 0xFFe4eff0;
	}
	@Override
	public int buttonTextOverColor() {
		return 0xFFFFFFFF;
	}

	@Override
	public int buttonTextDisabledColor() {
		return 0xFF002439;
	}

}
