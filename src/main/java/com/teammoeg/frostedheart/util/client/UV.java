/*
 * Copyright (c) 2021-2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.AbstractGui;

public class UV extends Rect {
	public enum Transition{
		UP,
		DOWN,
		LEFT,
		RIGHT;
	}
	final int textureW,textureH;
    public static UV delta(int x1, int y1, int x2, int y2) {
        return new UV(Rect.delta(x1, y1, x2, y2));
    }
    public static UV deltaWH(int x1, int y1, int x2, int y2,int tw,int th) {
        return new UV(Rect.delta(x1, y1, x2, y2), tw, th);
    }
    public UV(int x, int y, int w, int h) {
        super(x, y, w, h);
        textureW=256;
        textureH=256;
    }

    public UV(int x, int y, int w, int h, int textureW, int textureH) {
		super(x, y, w, h);
		this.textureW = textureW;
		this.textureH = textureH;
	}

	public UV(Rect r) {
        super(r);
        textureW=256;
        textureH=256;
    }

    public UV(Rect r, int textureW, int textureH) {
		super(r);
		this.textureW = textureW;
		this.textureH = textureH;
	}

	public UV(UV uv) {
        this(uv.x, uv.y, uv.w, uv.h,uv.textureW,uv.textureH);
    }
	
    //blit with width transition and  custom texture size
    public void blit(MatrixStack s, int targetX, int targetY, int sourceWidth, int sourceHeight) {
        AbstractGui.blit(s, targetX, targetY, x, y, sourceWidth, sourceHeight, textureW, textureH);
    }
    
    //blit with width transition and  custom texture size
    public void blit(MatrixStack s, int targetX, int targetY, int sourceWidth) {
        AbstractGui.blit(s, targetX, targetY, x, y, sourceWidth, h, textureW, textureH);
    }

    //normal blit
    public void blit(MatrixStack s, int targetX, int targetY) {
        AbstractGui.blit(s, targetX, targetY, x, y, w, h, textureW, textureH);
    }

    //blit with atlas
    public void blitAtlas(MatrixStack s, int targetX, int targetY, int gridX, int gridY) {
        AbstractGui.blit(s, targetX, targetY, x + gridX * w, y + gridY * h, w, h, textureW, textureH);
    }
    
    //blit with width transition add point
    public void blit(MatrixStack s, int targetX, int targetY, Point loc, int sourceWidth) {
        blit(s, targetX + loc.getX(), targetY + loc.getY(), sourceWidth);
    }

    //blit with width transition add point
    public void blit(MatrixStack s, int targetX, int targetY, Point loc, Transition direction,double progress) {
    	blit(s, targetX + loc.getX(), targetY + loc.getY(), direction, progress);
    }
    //blit with width transition add point
    public void blit(MatrixStack s, int targetX, int targetY, Transition direction,double progress) {
    	if(progress<0)
    		return;
    	if(progress>1)
    		progress=1;
    	switch(direction) {
    	case UP   :blit(s, targetX, targetY +(int)(h*(1-progress)), w, (int)(h*progress));return;
    	case LEFT :blit(s, targetX +(int)(w*(1-progress)), targetY, (int)(w*progress), h);return;
    	case DOWN :blit(s, targetX, targetY, w, (int)(h*progress));return;
    	case RIGHT:blit(s, targetX, targetY, (int)(w*progress), h);return;
    	}
    }
    //normal blit add point with custom texture size
    public void blitAt(MatrixStack s, int targetX, int targetY, Point loc) {
        blit(s, targetX + loc.getX(), targetY + loc.getY());
    }

    //blit with atlas and add point
    public void blitAtlas(MatrixStack s, int targetX, int targetY, Point loc, int gridX, int gridY) {
        blitAtlas(s, targetX + loc.getX(), targetY + loc.getY(), gridX, gridY);
    }
}
