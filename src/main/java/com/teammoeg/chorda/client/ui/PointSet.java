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

import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.util.IterateUtils;
import com.teammoeg.chorda.util.struct.MutablePair;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PointSet {
	List<Point> points;
	public PointSet(Point...points) {
		this.points=Arrays.asList(points);
	}
	public void drawUVs(List<TexturedUV> uvs,GuiGraphics stack,int x,int y) {
		for(MutablePair<TexturedUV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack, x, y, p.getSecond());
		}
	}
	public void drawUVs(List<UV> uvs,GuiGraphics stack,ResourceLocation texture,int x,int y) {
		for(MutablePair<UV, Point> p:IterateUtils.joinAnd(uvs, points)){
			p.getFirst().blit(stack,texture, x, y, p.getSecond());
		}
	}
}
