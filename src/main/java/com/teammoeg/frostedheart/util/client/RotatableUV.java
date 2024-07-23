package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.resources.ResourceLocation;

public class RotatableUV extends TexturedUV {
	int cX,cY;
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

	public void blitRotated(PoseStack matrixStack, int targetX, int targetY, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, cX, cY, degrees);
	}

	public void blitRotated(PoseStack matrixStack, int targetX, int targetY, Point loc, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, loc, cX, cY, degrees);
	}


}
