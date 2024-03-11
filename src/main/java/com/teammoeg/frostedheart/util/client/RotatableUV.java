package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.matrix.MatrixStack;

public class RotatableUV extends UV {
	int cX,cY;
	public RotatableUV(int x, int y, int w, int h,int cX, int cY) {
		super(x, y, w, h);
		this.cX=cX;
		this.cY=cY;
	}

	public RotatableUV(int x, int y, int w, int h,int cX, int cY, int textureW, int textureH) {
		super(x, y, w, h, textureW, textureH);
		this.cX=cX;
		this.cY=cY;
	}

	public void blitRotated(MatrixStack matrixStack, int targetX, int targetY, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, cX, cY, degrees);
	}

	public void blitRotated(MatrixStack matrixStack, int targetX, int targetY, Point loc, float degrees) {
		super.blitRotated(matrixStack, targetX, targetY, loc, cX, cY, degrees);
	}


}
