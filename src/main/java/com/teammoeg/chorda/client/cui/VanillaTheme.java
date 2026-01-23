package com.teammoeg.chorda.client.cui;

import com.teammoeg.frostedresearch.gui.TechIcons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;

public class VanillaTheme {
	static final ResourceLocation BEACON_LOCATION = new ResourceLocation("textures/gui/container/beacon.png");
	public VanillaTheme() {
	}
	public static void drawButton(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight,boolean enabled) {
		if(enabled) {
			if(isHighlight) {
				graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 86);
			}else {
				graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 66);
			}
		}else{
			graphics.blitNineSliced(AbstractWidget.WIDGETS_LOCATION, x, y, w, h, 20, 4, 200, 20, 0, 46); 
		}
	}
	public static void drawSliderBackground(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		int i = isHighlight? 0xFFFFFFFF : 0xFFA0A0A0;
		graphics.fill(x, y, x + w, y + h, i);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
	}
	public static void drawSliderBar(GuiGraphics graphics,int x,int y,int w,int h,boolean isHighlight) {
		graphics.fill(x, y, x + w, y + h, 0xFF808080);
		graphics.fill(x, y, x + w - 1, y + h - 1,0xFFC0C0C0);
		
	}
	public static void drawPanel(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.fill(x, y, x+w, y+h, 0xff8b8b8b);
	}
	public static void drawSlot(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.blit(BEACON_LOCATION, x-1, y-1, 35, 136, 18, 18);
	}
}
