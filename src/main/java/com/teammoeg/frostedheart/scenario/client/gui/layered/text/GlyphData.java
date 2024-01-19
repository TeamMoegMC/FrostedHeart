package com.teammoeg.frostedheart.scenario.client.gui.layered.text;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class GlyphData {
	int width;
	int height;
	int x;
	int y;
	int advance;
	int ascent;
	float scale=1;
	boolean hasAscent;
	BufferedImage image;

	public GlyphData(int x, int y, int width, int height, int i, int ascent,float scale) {
		super();
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		advance = i;
		this.ascent = ascent;
		hasAscent=true;
		this.scale=scale;
	}

	public int getBearingY() {
		//if(hasAscent)
		//	return 10 -  this.ascent+y;
		return y;
	}
	public int getBearingX() {
		return x;
	}
	public GlyphData(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

	public GlyphData() {
	}

	public int renderFont(Graphics2D g2d, int x, int y, int hsize) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		
		// AlphaComposite.
		g2d.drawImage(image, x, y, x + (int) (width * 1f / height * hsize*scale), y + (int)(hsize*scale), getBearingX(), getBearingY(), getBearingX() + width,
				getBearingY() + height, null);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		return (int) (advance * 1f / height * hsize);
	}

	public void parseSize(byte data) {
		int sx = (data >> 4) & 15;
		x += sx;
		width = (data & 15) + 1 - sx;
		height = 16;
		advance = width;
	}

	@Override
	public String toString() {
		return "GlyphData [width=" + width + ", height=" + height + ", x=" + x + ", y=" + y + ", advance=" + advance
				+ ", ascent=" + ascent + ", scale=" + scale + "]";
	}

}
