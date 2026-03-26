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

import java.util.Arrays;
import java.util.List;

import com.teammoeg.chorda.client.TesselateHelper.TextureTesselator;
import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.util.IterateUtils;
import com.teammoeg.chorda.util.struct.MutablePair;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 点集合类。存储一组点并提供在对应位置批量绘制UV纹理的方法。
 * <p>
 * Point set class. Stores a collection of points and provides methods to batch-draw UV textures at corresponding positions.
 */
public class PointSet {
	List<Point> points;

	/**
	 * 使用给定的点构造一个点集合。
	 * <p>
	 * Constructs a point set with the given points.
	 *
	 * @param points 点数组 / Array of points
	 */
	public PointSet(Point...points) {
		this.points=Arrays.asList(points);
	}
	/**
	 * 在对应的点位置批量绘制带纹理的UV列表。
	 * <p>
	 * Batch-draws a list of textured UVs at corresponding point positions.
	 *
	 * @param uvs 带纹理的UV列表 / List of textured UVs
	 * @param stack 图形上下文 / Graphics context
	 * @param x 基准X偏移 / Base X offset
	 * @param y 基准Y偏移 / Base Y offset
	 */
	public void drawUVs(List<TexturedUV> uvs,GuiGraphics stack,int x,int y) {
		for(MutablePair<TexturedUV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack, x, y, p.getSecond());
		}
	}
	/**
	 * 使用指定纹理在对应的点位置批量绘制UV列表。
	 * <p>
	 * Batch-draws a list of UVs at corresponding point positions using the specified texture.
	 *
	 * @param uvs UV列表 / List of UVs
	 * @param stack 图形上下文 / Graphics context
	 * @param texture 纹理资源位置 / Texture resource location
	 * @param x 基准X偏移 / Base X offset
	 * @param y 基准Y偏移 / Base Y offset
	 */
	public void drawUVs(List<UV> uvs,GuiGraphics stack,ResourceLocation texture,int x,int y) {
		for(MutablePair<UV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack,texture, x, y, p.getSecond());
		}
	}
	/**
	 * 在对应的点位置批量绘制带纹理的UV列表。
	 * <p>
	 * Batch-draws a list of textured UVs at corresponding point positions.
	 *
	 * @param uvs 带纹理的UV列表 / List of textured UVs
	 * @param stack 图形上下文 / Graphics context
	 * @param x 基准X偏移 / Base X offset
	 * @param y 基准Y偏移 / Base Y offset
	 */
	public void drawUVs(List<TexturedUV> uvs,TextureTesselator texture,GuiGraphics stack,int x,int y) {
		for(MutablePair<TexturedUV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().tesselate(texture,stack, x, y, p.getSecond());
		}
	}
}
