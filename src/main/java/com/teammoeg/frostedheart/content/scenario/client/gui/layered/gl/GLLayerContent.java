package com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl;

import com.teammoeg.frostedheart.content.scenario.client.gui.layered.OrderedRenderableContent;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;

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
			w=ClientUtils.mc().getWindow().getGuiScaledWidth();
		}
		this.width = w;
		if(h<0) {
			h=ClientUtils.mc().getWindow().getGuiScaledHeight();
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
		params.getGuiGraphics().enableScissor( params.getX(), params.getY(), params.getWidth(), params.getHeight());
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
