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
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;

public class TexturedUV extends UV {
    ResourceLocation texture;

    public TexturedUV(ResourceLocation texture, int x, int y, int w, int h) {
        super(x, y, w, h);
        this.texture = texture;
    }

    public TexturedUV(ResourceLocation texture, UV uv) {
        super(uv);
        this.texture = texture;
    }

    //blit with texture bind at IngameGui add point
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly, Point loc) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(mc.ingameGUI, s, lx, ly, loc);
    }

    //blit with texture bind at IngameGui
    public void blit(Minecraft mc, MatrixStack s, int lx, int ly) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(mc.ingameGUI, s, lx, ly);
    }

    //blit with texture bind add point
    public void blit(Minecraft mc, AbstractGui gui, MatrixStack s, int lx, int ly, Point loc) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(gui, s, lx, ly, loc);
    }

    //blit with texture bind
    public void blit(Minecraft mc, AbstractGui gui, MatrixStack s, int lx, int ly) {
        mc.getTextureManager().bindTexture(texture);
        super.blit(gui, s, lx, ly);
    }
}
