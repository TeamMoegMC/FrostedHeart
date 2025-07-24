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

package com.teammoeg.chorda.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.client.ui.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class IconCheckbox extends Checkbox {
    private static final FlatIcon SELECTED_ICON = FlatIcon.CHECK;
    private int scale;

    public IconCheckbox(int pX, int pY, Component pMessage, boolean pSelected) {
        this(pX, pY, 1, pMessage, pSelected);
    }

    public IconCheckbox(int pX, int pY, int scale, Component pMessage, boolean pSelected) {
        super(pX, pY, SELECTED_ICON.size.width, SELECTED_ICON.size.height, pMessage, pSelected, false);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
    }

    public void setScale(int scale) {
        int w = this.width / this.scale;
        int h = this.height / this.scale;
        this.scale = scale;
        this.width = w * scale;
        this.height = h * scale;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        FlatIcon icon = selected() ? SELECTED_ICON : FlatIcon.CROSS;
        RenderSystem.enableDepthTest();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        CGuiHelper.bindTexture(IconButton.ICON_LOCATION);
        CGuiHelper.blitColored(pGuiGraphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), IconButton.TEXTURE_WIDTH*scale, IconButton.TEXTURE_HEIGHT*scale, Colors.CYAN, alpha);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


}
