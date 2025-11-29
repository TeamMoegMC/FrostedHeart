/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.chorda.client.ui.Rect;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.GraphicGlyphRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;

public class GraphicsTextContent extends GraphicLayerContent {
	public int size;
	public boolean shadow;
	Component text;
	public GraphicsTextContent() {
	}
	public GraphicsTextContent(Component text,Rect rect,int size,boolean shadow) {
		super(rect.getX(), rect.getY(), rect.getW(), rect.getH());
		this.text=text;
		this.shadow=shadow;
		this.size=size;
	}
	public GraphicsTextContent(Component text,int x, int y, int w, int h,boolean shadow) {
		super(x, y, w, h);
		this.text=text;
		this.shadow=shadow;
	}

	public GraphicsTextContent(int x, int y, int width, int height, int z, float opacity, int size, boolean shadow,
			Component text) {
		super(x, y, width, height, z, opacity);
		this.size = size;
		this.shadow = shadow;
		this.text = text;
	}

	@Override
	public void tick() {
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsTextContent(x,y,width,height,z,opacity,size,shadow,text);
	}

	@Override
	public void prerender(PrerenderParams params) {
		Rect r=params.calculateRect(x,y,width,height);
		params.getG2d().setClip(r.getX(),r.getY(),r.getW(),r.getH());
		int size=(int) (r.getW()*1f/width*this.size);
		params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		ComponentRenderUtils.wrapComponents(text,(int) (r.getW()/(size/7f)), Minecraft.getInstance().font).get(0).accept(new GraphicGlyphRenderer(params.getG2d(),params.calculateScaledX(x),params.calculateScaledY(y),size,shadow));
		params.getG2d().setComposite(AlphaComposite.SrcOver);
		params.getG2d().setClip(0, 0, params.getWidth(),params.getHeight());
	}

}
