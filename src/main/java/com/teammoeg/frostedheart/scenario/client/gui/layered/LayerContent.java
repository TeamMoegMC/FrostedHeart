package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;

public abstract class LayerContent implements RenderableContent {



	int x,y,width,height,z,order;
	float opacity=1;
	public LayerContent() {
		this(0,0,-1,-1);
	}
	public LayerContent(int x, int y, int w, int h) {
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


	protected LayerContent(int x, int y, int width, int height, int z) {
		super();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.z = z;
	}
	@Override
	public final void render(RenderParams params) {
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

	public int getX() {
		return x;
	}


	public void setX(int x) {
		this.x = x;
	}


	public int getY() {
		return y;
	}


	public void setY(int y) {
		this.y = y;
	}


	public int getWidth() {
		return width;
	}


	public void setWidth(int width) {
		if(width<0) {
			width=ClientUtils.mc().getMainWindow().getScaledWidth();
		}
		this.width = width;
	}


	public int getHeight() {
		return height;
	}


	public void setHeight(int height) {
		if(height<0) {
			height=ClientUtils.mc().getMainWindow().getScaledHeight();
		}
		this.height = height;
	}


	public int getZ() {
		return z;
	}


	public void setZ(int z) {
		this.z = z;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}



}
