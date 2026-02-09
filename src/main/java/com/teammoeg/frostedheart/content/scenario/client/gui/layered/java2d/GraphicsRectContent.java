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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.Color;

import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;


public class GraphicsRectContent extends GraphicLayerContent {
	public int color;
	public GraphicsRectContent() {
	}

	public GraphicsRectContent(int color,int x, int y, int w, int h) {
		super(x, y, w, h);
		this.color=color;
	}

	public GraphicsRectContent(int x, int y, int width, int height, int z,float opacity) {
		super(x, y, width, height, z);
		this.opacity=opacity;
	}


	public GraphicsRectContent(int color, Rect rect) {
		this(color,rect.getX(),rect.getY(),rect.getW(),rect.getH());
	}

	@Override
	public void tick() {
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsRectContent(x,y,width,height,z,opacity);
	}

	@Override
	public void prerender(PrerenderParams params) {
		params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		params.getG2d().setColor(new Color(color,true));
		Rect r=params.calculateRect(x, y, width, height);
		params.getG2d().fillRect(r.getX(),r.getY(),r.getW(),r.getH());
		params.getG2d().setComposite(AlphaComposite.SrcOver);

	}

}
