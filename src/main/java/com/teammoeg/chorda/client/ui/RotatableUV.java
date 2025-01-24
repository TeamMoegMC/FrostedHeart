package com.teammoeg.chorda.client.ui;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class RotatableUV extends TexturedUV {
	final int cX,cY;
	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY) {
		super(rl, x, y, w, h);
		this.cX=cX;
		this.cY=cY;
	}

	public RotatableUV(ResourceLocation rl,int x, int y, int w, int h,int cX, int cY, int textureW, int textureH) {
		super(rl, x, y, w, h, textureW, textureH);
		this.cX=cX;
		this.cY=cY;
	}

	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, cX, cY, degrees);
	}

	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, Point loc, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, loc, cX, cY, degrees);
	}


}
