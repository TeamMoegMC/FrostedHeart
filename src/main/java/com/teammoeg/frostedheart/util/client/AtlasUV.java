/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.util.ResourceLocation;

public class AtlasUV extends TexturedUV {
	int gridW;
	int gridSize;
    public AtlasUV(ResourceLocation texture, int w, int h, int gridW, int gridSize) {
        super(texture, 0, 0, w, h);
        this.gridW=gridW;
        this.gridSize=gridSize;
    }
    public AtlasUV(ResourceLocation texture, int w, int h, int gridW, int gridSize, int tw, int th) {
        super(texture, 0, 0, w, h, tw, th);
        this.gridW=gridW;
        this.gridSize=gridSize;
    }
	public AtlasUV(ResourceLocation texture, int x, int y, int w, int h, int gridW, int gridSize, int tw, int th) {
		super(texture, x, y, w, h, tw, th);
		this.gridW=gridW;
		this.gridSize=gridSize;
	}
	public AtlasUV(String texture, int x, int y, int w, int h, int gridW, int gridSize, int tw, int th) {
		super(texture, x, y, w, h, tw, th);
		this.gridW=gridW;
		this.gridSize=gridSize;
	}
	public AtlasUV(String texture, int w, int h, int gridW, int gridSize, int tw, int th) {
		super(texture, 0, 0, w, h, tw, th);
		this.gridW=gridW;
		this.gridSize=gridSize;
	}
	public void blitAtlas(MatrixStack s, int targetX, int targetY, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, gridIndex % gridW, gridIndex / gridW);
	}
	public void blitAtlas(MatrixStack s, int targetX, int targetY, Point loc, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, loc, gridIndex % gridW, gridIndex / gridW);
	}
	public void blitAtlasVH(MatrixStack s, int targetX, int targetY, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, gridIndex / gridW, gridIndex % gridW);
	}
	public void blitAtlasVH(MatrixStack s, int targetX, int targetY, Point loc, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, loc, gridIndex / gridW, gridIndex % gridW);
	}
	Map<Point,UV> cache=new HashMap<>();
	public UV childAtlas(int gridX,int gridY) {
		return cache.computeIfAbsent(new Point(gridX, gridY), px->new TexturedUV(texture, x + px.getX() * w, y + px.getY() * h, w, h, textureW, textureH));
	}
	public UV childAtlas(int gridIndex) {
		return childAtlas(gridIndex % gridW, gridIndex / gridW);
	}
	public UV childAtlasVH(int gridIndex) {
		return childAtlas(gridIndex / gridW, gridIndex % gridW);
	}
}
