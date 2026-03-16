/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.client.ui;

import java.util.HashMap;
import java.util.Map;

import com.teammoeg.chorda.math.Point;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 纹理图集UV坐标。扩展TexturedUV以支持基于网格索引的图集渲染，可按行列或索引访问图集中的单元格。
 * <p>
 * Atlas UV coordinates. Extends TexturedUV to support grid-index-based atlas rendering,
 * allowing access to atlas cells by row/column or linear index.
 */
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
	/**
	 * 按线性索引绘制图集中的单元格（水平优先排列）。
	 * <p>
	 * Draws an atlas cell by linear index (horizontal-first layout).
	 *
	 * @param s 图形上下文 / the graphics context
	 * @param targetX 目标X坐标 / the target X coordinate
	 * @param targetY 目标Y坐标 / the target Y coordinate
	 * @param gridIndex 网格线性索引 / the grid linear index
	 */
	public void blitAtlas(GuiGraphics s, int targetX, int targetY, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, gridIndex % gridW, gridIndex / gridW);
	}
	/**
	 * 按线性索引绘制图集中的单元格，附加位置偏移（水平优先排列）。
	 * <p>
	 * Draws an atlas cell by linear index with position offset (horizontal-first layout).
	 *
	 * @param s 图形上下文 / the graphics context
	 * @param targetX 目标X坐标 / the target X coordinate
	 * @param targetY 目标Y坐标 / the target Y coordinate
	 * @param loc 位置偏移点 / the position offset point
	 * @param gridIndex 网格线性索引 / the grid linear index
	 */
	public void blitAtlas(GuiGraphics s, int targetX, int targetY, Point loc, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, loc, gridIndex % gridW, gridIndex / gridW);
	}
	/**
	 * 按线性索引绘制图集中的单元格（垂直优先排列）。
	 * <p>
	 * Draws an atlas cell by linear index (vertical-first layout).
	 *
	 * @param s 图形上下文 / the graphics context
	 * @param targetX 目标X坐标 / the target X coordinate
	 * @param targetY 目标Y坐标 / the target Y coordinate
	 * @param gridIndex 网格线性索引 / the grid linear index
	 */
	public void blitAtlasVH(GuiGraphics s, int targetX, int targetY, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, gridIndex / gridW, gridIndex % gridW);
	}
	/**
	 * 按线性索引绘制图集中的单元格，附加位置偏移（垂直优先排列）。
	 * <p>
	 * Draws an atlas cell by linear index with position offset (vertical-first layout).
	 *
	 * @param s 图形上下文 / the graphics context
	 * @param targetX 目标X坐标 / the target X coordinate
	 * @param targetY 目标Y坐标 / the target Y coordinate
	 * @param loc 位置偏移点 / the position offset point
	 * @param gridIndex 网格线性索引 / the grid linear index
	 */
	public void blitAtlasVH(GuiGraphics s, int targetX, int targetY, Point loc, int gridIndex) {
		if(gridIndex>=gridSize)
			gridIndex=gridSize-1;
		if(gridIndex<0)return;
		super.blitAtlas(s, targetX, targetY, loc, gridIndex / gridW, gridIndex % gridW);
	}
	Map<Point,UV> cache=new HashMap<>();
	/**
	 * 获取图集中指定网格位置的子UV，结果会被缓存。
	 * <p>
	 * Gets a child UV at the specified grid position in the atlas. Results are cached.
	 *
	 * @param gridX 网格X索引 / the grid X index
	 * @param gridY 网格Y索引 / the grid Y index
	 * @return 对应网格位置的UV / the UV at the specified grid position
	 */
	public UV childAtlas(int gridX,int gridY) {
		return cache.computeIfAbsent(new Point(gridX, gridY), px->new TexturedUV(texture, x + px.getX() * w, y + px.getY() * h, w, h, textureW, textureH));
	}
	/**
	 * 按线性索引获取图集中的子UV（水平优先排列）。
	 * <p>
	 * Gets a child UV by linear index in the atlas (horizontal-first layout).
	 *
	 * @param gridIndex 网格线性索引 / the grid linear index
	 * @return 对应位置的UV / the UV at the specified position
	 */
	public UV childAtlas(int gridIndex) {
		return childAtlas(gridIndex % gridW, gridIndex / gridW);
	}
	/**
	 * 按线性索引获取图集中的子UV（垂直优先排列）。
	 * <p>
	 * Gets a child UV by linear index in the atlas (vertical-first layout).
	 *
	 * @param gridIndex 网格线性索引 / the grid linear index
	 * @return 对应位置的UV / the UV at the specified position
	 */
	public UV childAtlasVH(int gridIndex) {
		return childAtlas(gridIndex / gridW, gridIndex % gridW);
	}
}
