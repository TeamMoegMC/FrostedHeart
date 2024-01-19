package com.teammoeg.frostedheart.scenario.client.gui.layered.text;

import java.awt.Color;
import java.awt.Graphics2D;

import net.minecraft.util.ICharacterConsumer;
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
		
		if(p_accept_2_.getColor()!=null) {
			int c=p_accept_2_.getColor().getColor();
			//g2d.setXORMode(new java.awt.Color(c.getColor()>>16,c.getColor()>>8,c.getColor(),c.getColor()>>24));
		}
		g2d.setColor(new Color(0xff,0x00,0x00,0xff));
		GlyphData glyph=KGlyphProvider.INSTANCE.getGlyph(p_accept_3_);
		System.out.println(glyph);
		x+=glyph.renderFont(g2d, x, y, size);
		g2d.setColor(new Color(0xff,0xff,0xff,0xff));
		return true;
	}

}
