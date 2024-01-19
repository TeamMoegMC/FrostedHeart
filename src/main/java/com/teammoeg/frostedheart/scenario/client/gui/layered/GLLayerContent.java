package com.teammoeg.frostedheart.scenario.client.gui.layered;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;

public abstract class GLLayerContent extends OrderedRenderableContent {


	float x,y,width,height,opacity=1;
	public GLLayerContent() {
		this(0,0,-1,-1);
	}
	public GLLayerContent(float x, float y, float w, float h) {
		super();
		this.x = x;
		this.y = y;
		if(w<0) {
			w=ClientUtils.mc().getMainWindow().getScaledWidth();
		}
		this.width = w;
		if(h<0) {
			h=ClientUtils.mc().getMainWindow().getScaledHeight();
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
		GuiHelper.pushScissor(params.getMinecraft().getMainWindow(), params.getX(), params.getY(), params.getWidth(), params.getHeight());
		this.renderContents(params);
		GuiHelper.popScissor(params.getMinecraft().getMainWindow());
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
