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
	static final GlyphData EMPTY = new GlyphData(0, 0, 7, 9, 5, 0, 1, false, null, -1);

	private final int width;
	private final int height;
	private final int x;
	private final int y;
	private final int advance;
	private final int ascent;
	private final float scale;
	private final boolean hasAscent;
	private final boolean unicode;
	private final BufferedImage image;
	private final int cacheId;

	private GlyphData(int x, int y, int width, int height, int advance, int ascent, float scale,
			boolean unicode, BufferedImage image, int cacheId) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		this.advance = advance;
		this.ascent = ascent;
		this.hasAscent = ascent != 0;
		this.scale = scale;
		this.unicode = unicode;
		this.image = image;
		this.cacheId = cacheId;
	}

	static GlyphData bitmap(int cacheId, int x, int y, int width, int height, int advance, int ascent,
			float scale, BufferedImage image) {
		return new GlyphData(x, y, width, height, advance, ascent, scale, false, image, cacheId);
	}

	static GlyphData legacyUnicode(int cacheId, int x, int y, byte packedSize, BufferedImage image) {
		int left = (packedSize >> 4) & 15;
		int width = (packedSize & 15) + 1 - left;
		return new GlyphData(x + left, y, width, 16, width + 2, 0, 1, true, image, cacheId);
	}

	static GlyphData space(int cacheId, int advance) {
		return new GlyphData(0, 0, 0, 16, advance, 0, 1, false, null, cacheId);
	}

	int renderFont(Graphics2D graphics, GlyphImageCache cache, int drawX, int drawY, int targetHeight, int color) {
		Object originalHint = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		if (!unicode) {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		}
		if (image == null) {
			return scaledAdvance(targetHeight);
		}

		int sourceX = x;
		int sourceY = y;
		BufferedImage currentImage = image;
		if (color != 0xFFFFFFFF) {
			currentImage = cache.get(this, color);
			sourceX = 0;
			sourceY = 0;
		}
		graphics.drawImage(currentImage, drawX, drawY,
				drawX + (int) (width * 1F / height * targetHeight * scale),
				drawY + (int) (targetHeight * scale),
				sourceX, sourceY, sourceX + width, sourceY + height, null);
		if (!unicode) {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, originalHint);
		}
		return scaledAdvance(targetHeight);
	}

	BufferedImage createColoredImage(int color) {
		BufferedImage colored = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int pixelX = 0; pixelX < width; pixelX++) {
			for (int pixelY = 0; pixelY < height; pixelY++) {
				if ((image.getRGB(pixelX + x, pixelY + y) & 0xFF000000) != 0) {
					colored.setRGB(pixelX, pixelY, color);
				}
			}
		}
		return colored;
	}

	int scaledAdvance(int targetHeight) {
		return (int) (advance * 1F / height * targetHeight);
	}

	int height() {
		return height;
	}

	boolean isUnicode() {
		return unicode;
	}

	int cacheId() {
		return cacheId;
	}

	@Override
	public String toString() {
		return "GlyphData [width=" + width + ", height=" + height + ", x=" + x + ", y=" + y + ", advance=" + advance
				+ ", ascent=" + ascent + ", scale=" + scale + "]";
	}

}
