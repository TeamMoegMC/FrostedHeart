/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AtlasUV extends TexturedUV {
    public AtlasUV(ResourceLocation texture, int w, int h) {
        super(texture, 0, 0, w, h);
    }

    public AtlasUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(texture, x, y, w, h);
    }

    //blit with texture bind and altas set add point
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, Point loc, int mx, int my, int p3, int p4) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(s, lx, ly, loc, mx, my, p3, p4);
    }

    //blit with texture bind and altas set
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, int mx, int my, int p3, int p4) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(s, lx, ly, mx, my, p3, p4);
    }
}
