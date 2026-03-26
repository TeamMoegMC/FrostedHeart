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

package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.TextureTesselator;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.icon.CIcons.CTextureIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class LineIcon extends CTextureIcon {
    int h;
    int w;
    int side1;
    int side2;
    CTextureIcon s0;
    CTextureIcon m;
    CTextureIcon s1;
    CTextureIcon full;
    public LineIcon(CTextureIcon i, int x, int y, int w, int h, int side1, int side2, int tw, int th) {
    	full=i.withUV(x, y, w, h, tw, th);
        this.h = h;
        this.w = w;
        this.side1 = side1;
        this.side2 = side2;
        updateParts();
    }


    @Override
    public void draw(GuiGraphics matrixStack, int x, int y, int w, int h) {
    	try(TextureTesselator tex=TesselateHelper.getTextureTesselator(full.getTexture())){
			tesselate(tex,matrixStack,x,y,w,h);
		}
       
    }

	@Override
	public void tesselate(TextureTesselator tex, GuiGraphics ms, int x, int y, int w, int h) {
		 int msize = w - side2 - side1;
	        if (msize <= 0) {
	            s0.tesselate(tex, ms, x, y, side1, h);
	            s1.tesselate(tex, ms, x + side1, y, side2, h);
	        } else {
	            m .tesselate(tex, ms, x + side1, y, msize, h);
	            s0.tesselate(tex, ms, x, y, side1, h);
	            s1.tesselate(tex, ms, x + w - side2, y, side2, h);
	        }
	}

    public void updateParts() {
        s0 = full.asPart(0, 0, side1, h);
        m = full.asPart(side1, 0, w - side2 - side1, h);
        s1 = full.asPart(this.w - side2, 0, side2, h);
    }


	@Override
	public boolean isEmpty() {
		return true;
	}


	@Override
	public CTextureIcon withUV(int x, int y, int w, int h, int tw, int th) {
		return full.withUV(x, y, w, h, tw, th);
	}


	@Override
	public CTextureIcon toNineSlice(int c) {
		return full.toNineSlice(c);
	}


	@Override
	public CTextureIcon asPart(int x, int y, int w, int h) {
		return full.asPart(x, y, w, h);
	}


	@Override
	public ResourceLocation getTexture() {
		return full.getTexture();
	}


}
