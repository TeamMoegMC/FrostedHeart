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

package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class TechButton extends Button {

    public TechButton(UIElement panel) {
        super(panel);
    }

    public TechButton(UIElement panel, CIcon i) {
        super(panel);
        super.setIcon(i);
    }

    public TechButton(UIElement panel, Component t, CIcon i) {
        super(panel, t, i);
    }
    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        CGuiHelper.resetGuiDrawing();;
        DrawDeskTheme.drawButton(matrixStack, x, y, w, h, isMouseOver());

        if (hasIcon()) {
            drawIcon(matrixStack, x + (w - 16) / 2, y + (h - 16) / 2, 16, 16);
        }
    }

    public boolean hasIcon() {
        return icon != null && !icon.isEmpty();
    }

}
