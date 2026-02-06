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

import com.teammoeg.chorda.client.cui.LayerScrollBar;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;

public class TechScrollBar extends LayerScrollBar {

    boolean isHidden = false;

    public TechScrollBar(UIElement parent, UILayer panel) {
        super(parent, panel);
    }

    public TechScrollBar(UIElement parent,  boolean isVertical, UILayer p) {
        super(parent, isVertical, p);
    }

    @Override
    public boolean isScrollFocused() {
        return super.isScrollFocused() && getAffectedLayer().isEnabled() && !isHidden;
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        if (!isHidden) {
            CGuiHelper.resetGuiDrawing();
            DrawDeskTheme.drawSliderBackground(matrixStack, x, y, w, h,isMouseOver());
        }
    }

    @Override
    public void drawScrollBar(GuiGraphics matrixStack, int x, int y, int w, int h) {
        if (!isHidden)
        	DrawDeskTheme.drawSliderBar(matrixStack, x, y, w, h, isMouseOver());
            //super.drawScrollBar(matrixStack, x + 1, y + 1, w - 2, h - 2);
    }

    public void hide() {
        this.isHidden = true;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void unhide() {
        this.isHidden = false;
    }
}
