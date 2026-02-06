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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.ui.Rect;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.OrderedRenderableContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;

public abstract class GLLayerContent extends OrderedRenderableContent {


	protected float x,y;
	protected float width;
	protected float height;
	protected float opacity=1;
	public GLLayerContent() {
		this(0,0,-1,-1);
	}
	public GLLayerContent(float x, float y, float w, float h) {
		super();
		this.x = x;
		this.y = y;
		if(w<0) {
			w=ClientUtils.getMc().getWindow().getGuiScaledWidth();
		}
		this.width = w;
		if(h<0) {
			h=ClientUtils.getMc().getWindow().getGuiScaledHeight();
		}
		this.height = h;
	}

	protected GLLayerContent(float x, float y, float width, float height, int z) {
		super();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.z = z;
	}
	@Override
	public void render(RenderParams params) {
		params=params.copyWithCurrent(this);
		params.getGuiGraphics().enableScissor( params.getX(), params.getY(), params.getX()+params.getWidth(), params.getY()+params.getHeight());
		this.renderContents(params);
		params.getGuiGraphics().disableScissor();
	}
	public float getOpacity() {
		return opacity;
	}
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	public abstract void renderContents(RenderParams params);

	

	public void setWidth(float width) {
		if(width<0) {
			width=1;
		}
		this.width = width;
	}



	public void setHeight(float height) {
		if(height<0) {
			height=1;
		}
		this.height = height;
	}

	@Override
	public void prerender(PrerenderParams params) {
	}
	public float getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public float getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	public float getWidth() {
		return width;
	}
	public float getHeight() {
		return height;
	}



}
