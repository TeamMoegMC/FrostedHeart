package com.teammoeg.frostedheart.scenario.client.gui.layered;

public abstract class GraphicLayerContent extends OrderedRenderableContent {


	int x,y,width,height;
	float opacity=1;
	public GraphicLayerContent() {
		this(0,0,-1,-1);
	}
	public GraphicLayerContent(int x, int y, int w, int h) {
		super();
		this.x = x;
		this.y = y;
		if(w<0) {
			w=2048;
		}
		this.width = w;
		if(h<0) {
			h=1152;
		}
		this.height = h;
	}


	protected GraphicLayerContent(int x, int y, int width, int height, int z) {
		super();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.z = z;
	}
	@Override
	public void render(RenderParams params) {
	}
	public float getOpacity() {
		return opacity;
	}
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	public void setWidth(int width) {
		if(width<0) {
			width=2048;
		}
		this.width = width;
	}
	public void setHeight(int height) {
		if(height<0) {
			height=1152;
		}
		this.height = height;
	}
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
	public int getHeight() {
		return height;
	}



}
