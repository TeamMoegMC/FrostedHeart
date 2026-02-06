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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.font;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

import net.minecraft.util.FormattedCharSink;
import net.minecraft.network.chat.Style;

public class GraphicGlyphRenderer implements FormattedCharSink{
	Graphics2D g2d;
	int x;
	public GraphicGlyphRenderer(Graphics2D g2d, int x, int y, int size, boolean shadow) {
		super();
		this.g2d = g2d;
		this.x = x;
		this.y = y;
		this.size = size;
		this.shadow = shadow;
	}


	int y;
	public int size=18;
	public boolean shadow=true;
	static AffineTransform italic = new AffineTransform();
	static AffineTransform empty = new AffineTransform();

	static {
		italic.shear(-.2, 0);
	}


	@Override
	public boolean accept(int p_accept_1_, Style p_accept_2_, int p_accept_3_) {
		int c=0xFFFFFFFF;
		if(p_accept_2_.getColor()!=null) {
			c=p_accept_2_.getColor().getValue();
			//g2d.setXORMode(new java.awt.Color(c.getColor()>>16,c.getColor()>>8,c.getColor(),c.getColor()>>24));
		}
		if((c&0xFF000000)==0)
			c|=0xFF000000;

		GlyphData glyph=KGlyphProvider.INSTANCE.getGlyph(p_accept_3_);
		if(p_accept_2_.isObfuscated()) {
			
		}
		if(glyph==null||p_accept_3_==32) {
			glyph=GlyphData.EMPTY;
		}
		int advance=0;
		empty.setTransform(g2d.getTransform());
		if(p_accept_2_.isItalic())
			g2d.setTransform(italic);
		if(shadow) {
			int shadowOff=Math.round((glyph.isUnicode?0.5f:1f)/ glyph.height*size);
			glyph.renderFont(g2d, x+shadowOff, y+shadowOff, size,0xFF000000);
			advance++;
		}
		advance+=glyph.renderFont(g2d, x, y, size,c);
		if(p_accept_2_.isBold()) {
			int offset=Math.round((glyph.isUnicode?0.5f:1f)/ glyph.height*size);
			glyph.renderFont(g2d, x+offset, y, size,c);
			advance+=1;
		}
		Color prev=g2d.getColor();
		g2d.setColor(new Color(c,true));
		
		g2d.setTransform(empty);
		Stroke sp=g2d.getStroke();
		if(p_accept_2_.isStrikethrough()) {
			int cy=(int) (y+0.5*size+1);
			g2d.setStroke(new BasicStroke(3));
			g2d.drawLine(x-1, cy, x+advance+2, cy);
		}
		if(p_accept_2_.isUnderlined()) {
			int cy= y+size+3;
			g2d.setStroke(new BasicStroke(3));
			g2d.drawLine(x-1, cy, x+advance+2, cy);
		}
		g2d.setColor(prev);
		g2d.setStroke(sp);
		x+=advance;
		return true;
	}

}
