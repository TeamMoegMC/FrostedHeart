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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class GlyphData {
	static GlyphData EMPTY=new GlyphData(0,0,7,9,5,0,1) {

		@Override
		public int renderFont(Graphics2D g2d, int x, int y, int hsize, int color) {
			return (int) (advance * 1f / height * hsize);
		}
		
	};
	int width;
	int height;
	int x;
	int y;
	int advance;
	int ascent;
	float scale=1;
	boolean hasAscent;
	public boolean isUnicode=false;
	BufferedImage image;
	int cachedColor=0xFFFFFFFF;
	BufferedImage chachedGraphics;
	BufferedImage shadowGraphics;
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

	public GlyphData(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

	public GlyphData() {
	}

	public int renderFont(Graphics2D g2d, int x, int y, int hsize,int color) {
		if(!isUnicode)
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		int crx=this.x;
		int cry=this.y;
		BufferedImage currentImage=image;
		if(image==null)
			return (int) (advance * 1f / height * hsize);
		if(color!=0xFFFFFFFF) {
			if(color==0xFF000000) {
				if(shadowGraphics==null) {
					shadowGraphics=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					for (int x0 = 0; x0 < width; x0++) {
						for (int y0 = 0; y0 < height; y0++) {
							int srgb = image.getRGB(x0+this.x, y0+this.y);
							if ((srgb&0xFF000000) != 0) {
								shadowGraphics.setRGB(x0, y0, color);
							}
						}
					}
				}
				crx=cry=0;
				currentImage=shadowGraphics;
			}else if(color!=cachedColor) {
				if(chachedGraphics==null) {
					chachedGraphics=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				}
				for (int x0 = 0; x0 < width; x0++) {
					for (int y0 = 0; y0 < height; y0++) {
						int srgb = image.getRGB(x0+this.x, y0+this.y);
						if ((srgb&0xFF000000) != 0) {
							chachedGraphics.setRGB(x0, y0, color);
						}
					}
				}
				crx=cry=0;
				currentImage=chachedGraphics;
			}else {
				crx=cry=0;
				currentImage=chachedGraphics;
			}
		}

		// AlphaComposite.
		g2d.drawImage(currentImage, x, y, x + (int) (width * 1f / height * hsize*scale), y + (int)(hsize*scale),crx , cry, crx + width,
				cry + height, null);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		return (int) (advance * 1f / height * hsize);
	}

	public void parseSize(byte data) {
		int sx = (data >> 4) & 15;
		x += sx;
		width = (data & 15) + 1-sx;
		height = 16;
		advance = width+2;
	}

	@Override
	public String toString() {
		return "GlyphData [width=" + width + ", height=" + height + ", x=" + x + ", y=" + y + ", advance=" + advance
				+ ", ascent=" + ascent + ", scale=" + scale + "]";
	}

}
