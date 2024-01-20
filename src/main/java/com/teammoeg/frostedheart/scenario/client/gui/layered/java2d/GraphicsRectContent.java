package com.teammoeg.frostedheart.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.Color;
import com.teammoeg.frostedheart.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.scenario.client.gui.layered.RenderableContent;

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
		
		params.getG2d().fillRect(x, y, width, height);
		params.getG2d().setComposite(AlphaComposite.SrcOver);

	}

}
