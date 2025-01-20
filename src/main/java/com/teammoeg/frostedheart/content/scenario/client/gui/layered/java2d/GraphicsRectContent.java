package com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.Color;

import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.chorda.util.client.Rect;

import dev.ftb.mods.ftblibrary.icon.Color4I;

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


	public GraphicsRectContent(Color4I color, Rect rect) {
		this(color.rgba(),rect.getX(),rect.getY(),rect.getW(),rect.getH());
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
