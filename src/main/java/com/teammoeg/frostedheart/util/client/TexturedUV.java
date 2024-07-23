/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.FHMain;

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
    public TexturedUV(String texture, int x, int y, int w, int h) {
        this(new ResourceLocation(FHMain.MODID,"textures/gui/"+texture), x, y, w, h);
    }
    public TexturedUV(String texture, int x, int y, int w, int h, int tw, int th) {
    	this(new ResourceLocation(FHMain.MODID,"textures/gui/"+texture), x, y, w, h, tw, th);
    }
    public TexturedUV(String texture, UV uv) {
    	this(new ResourceLocation(FHMain.MODID,"textures/gui/"+texture), uv);
    }
    protected void bindTexture() {
    	ClientUtils.mc().getTextureManager().bind(texture);
    }

	@Override
	public void blit(PoseStack s, int targetX, int targetY, int sourceWidth, int sourceHeight) {
		bindTexture();
		super.blit(s, targetX, targetY, sourceWidth, sourceHeight);
	}

	@Override
	public void blit(PoseStack s, int targetX, int targetY, int sourceWidth) {
		bindTexture();
		super.blit(s, targetX, targetY, sourceWidth);
	}

	@Override
	public void blit(PoseStack s, int targetX, int targetY) {
		bindTexture();
		super.blit(s, targetX, targetY);
	}

	@Override
	public void blitAtlas(PoseStack s, int targetX, int targetY, int gridX, int gridY) {
		bindTexture();
		super.blitAtlas(s, targetX, targetY, gridX, gridY);
	}

	@Override
	public void blit(PoseStack s, int targetX, int targetY, Point loc, int sourceWidth) {
		bindTexture();
		super.blit(s, targetX, targetY, loc, sourceWidth);
	}

	@Override
	public void blitAt(PoseStack s, int targetX, int targetY, Point loc) {
		bindTexture();
		super.blitAt(s, targetX, targetY, loc);
	}

	@Override
	public void blitAtlas(PoseStack s, int targetX, int targetY, Point loc, int gridX, int gridY) {
		bindTexture();
		super.blitAtlas(s, targetX, targetY, loc, gridX, gridY);
	}
	@Override
	public void blit(PoseStack s, int targetX, int targetY, Point loc, Transition direction, double progress) {
		bindTexture();
		super.blit(s, targetX, targetY, loc, direction, progress);
	}
	@Override
	public void blit(PoseStack s, int targetX, int targetY, Transition direction, double progress) {
		bindTexture();
		super.blit(s, targetX, targetY, direction, progress);
	}

}
