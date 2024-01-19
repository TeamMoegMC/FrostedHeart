package com.teammoeg.frostedheart.scenario.client.gui.layered.text;

import java.awt.Graphics2D;

import net.minecraft.util.ICharacterConsumer;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.Style;

public class GraphicGlyphRenderer implements ICharacterConsumer{
	int x;
	Graphics2D g2d;
	int y;
	int size=18;
	public GraphicGlyphRenderer(int x, Graphics2D g2d, int y) {
		super();
		this.x = x;
		this.g2d = g2d;
		this.y = y;
	}

	@Override
	public boolean accept(int p_accept_1_, Style p_accept_2_, int p_accept_3_) {
		Color c=p_accept_2_.getColor();
		if(c!=null) {
			//g2d.setXORMode(new java.awt.Color(c.getColor()>>16,c.getColor()>>8,c.getColor(),c.getColor()>>24));
		}
		x+=KGlyphProvider.INSTANCE.getGlyph(p_accept_3_).renderFont(g2d, x, y, size);
		return false;
	}

}
