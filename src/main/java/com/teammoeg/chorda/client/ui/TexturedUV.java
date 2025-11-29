/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TexturedUV extends UV {
    ResourceLocation texture;

    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.texture = texture;
    }
    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h, int tw, int th) {
        super(x, y, w, h, tw, th);
        this.texture = texture;
    }
    public TexturedUV(ResourceLocation texture, UV uv) {
        super(uv);
        this.texture = texture;
    }
	public void blit(GuiGraphics s, int targetX, int targetY, int sourceWidth, int sourceHeight) {
		super.blit(s,texture, targetX, targetY, sourceWidth, sourceHeight);
	}

	public void blit(GuiGraphics s, int targetX, int targetY, int sourceWidth) {
		super.blit(s,texture, targetX, targetY, sourceWidth);
	}

	public void blit(GuiGraphics s, int targetX, int targetY) {
		super.blit(s,texture, targetX, targetY);
	}

	public void blitAtlas(GuiGraphics s, int targetX, int targetY, int gridX, int gridY) {
		super.blitAtlas(s,texture, targetX, targetY, gridX, gridY);
	}

	public void blit(GuiGraphics s, int targetX, int targetY, Point loc, int sourceWidth) {
		super.blit(s,texture, targetX, targetY, loc, sourceWidth);
	}

	public void blitAt(GuiGraphics s, int targetX, int targetY, Point loc) {
		super.blitAt(s,texture, targetX, targetY, loc);
	}

	public void blitAtlas(GuiGraphics s, int targetX, int targetY, Point loc, int gridX, int gridY) {
		super.blitAtlas(s,texture, targetX, targetY, loc, gridX, gridY);
	}
	public void blit(GuiGraphics s, int targetX, int targetY, Point loc, Transition direction, double progress) {
		super.blit(s,texture, targetX, targetY, loc, direction, progress);
	}
	public void blit(GuiGraphics s, int targetX, int targetY, Transition direction, double progress) {
		super.blit(s,texture, targetX, targetY, direction, progress);
	}
	public void blitRotated(GuiGraphics graphics,int targetX, int targetY, int centerX, int centerY, float degrees) {
		super.blitRotated(graphics, texture, targetX, targetY, centerX, centerY, degrees);
	}
	public void blitRotated(GuiGraphics matrixStack, int targetX, int targetY, Point loc, int centerX, int centerY, float degrees) {
		super.blitRotated(matrixStack, texture, targetX, targetY, loc, centerX, centerY, degrees);
	}
	public void blitCenter(GuiGraphics s,  int centerX, int centerY) {
		super.blitCenter(s, texture, centerX, centerY);
	}
	public void blitCenter(GuiGraphics s,  int centerX, int centerY, Point loc) {
		super.blitCenter(s, texture, centerX, centerY, loc);
	}
	public void blit(GuiGraphics s, int targetX, int targetY, Point loc) {
		super.blit(s, texture, targetX, targetY, loc);
	}
    public static TexturedUV deltaWH(ResourceLocation texture,int x1, int y1, int x2, int y2,int tw,int th) {
        return new TexturedUV(texture,UV.deltaWH(x1, y1, x2, y2, tw, th));
    }
}
