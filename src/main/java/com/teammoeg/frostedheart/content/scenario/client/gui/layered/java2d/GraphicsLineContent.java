package com.teammoeg.frostedheart.content.scenario.client.gui.layered.java2d;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;

import com.teammoeg.frostedheart.content.scenario.client.gui.layered.PrerenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;
import com.teammoeg.chorda.util.client.Point;

import dev.ftb.mods.ftblibrary.icon.Color4I;

public class GraphicsLineContent extends GraphicLayerContent {
	public int color;
	public int wid;
	public GraphicsLineContent() {
	}

	public GraphicsLineContent(int color,int x, int y, int w, int h) {
		super(x, y, w, h);
		this.color=color;
	}

	public GraphicsLineContent(int x, int y, int width, int height, int z,float opacity) {
		super(x, y, width, height, z);
		this.opacity=opacity;
	}

	public GraphicsLineContent(Color4I color,Point start,Point end) {
		super(start.getX(), start.getY(), end.getX(), end.getY());
		this.color=color.rgba();
	}
	@Override
	public void tick() {
	}

	@Override
	public RenderableContent copy() {
		return new GraphicsLineContent(x,y,width,height,z,opacity);
	}

	@Override
	public void prerender(PrerenderParams params) {
		params.getG2d().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		params.getG2d().setColor(new Color(color,true));
		params.getG2d().setStroke(new BasicStroke(params.calculateScaledSize(wid)));
		params.getG2d().setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		//System.out.println(String.format("Drawing line %s,%s->%s,%s %s", params.calculateScaledX(x),params.calculateScaledY(y), params.calculateScaledX(width), params.calculateScaledY(height),params.calculateScaledSize(wid)));
		params.getG2d().drawLine(params.calculateScaledX(x),params.calculateScaledY(y), params.calculateScaledX(width), params.calculateScaledY(height));
		params.getG2d().setRenderingHint(
			    RenderingHints.KEY_ANTIALIASING,
			    RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		params.getG2d().setComposite(AlphaComposite.SrcOver);

	}

}
