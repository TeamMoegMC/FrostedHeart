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

package com.teammoeg.frostedheart.content.ui.tips.client.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;

public class TipTheme implements Theme {
    public TipLayer tipLayer;

    public int getFontColor() {
        return tipLayer != null ? Colors.cyanToTheme(tipLayer.display.fontColor()) : Colors.CYAN;
    }

    public int getBackgroundColor() {
        float alpha = ClientUtils.getMc().screen == null ? 0.55F : 0.8F;
        return tipLayer != null ? Colors.setAlpha(tipLayer.display.backgroundColor(), alpha) : Colors.setAlpha(UIBGColor(), 0.5F);
    }

    public TipTheme(TipLayer tipLayer) {
        this.tipLayer = tipLayer;
    }
    public TipTheme() {}

    @Override
    public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
        if (isHighlight) {
            graphics.fill(x, y, x+w, y+h, Colors.setAlpha(Colors.makeDark(getFontColor(), 0.3F), 0.5F));
        }
    }

    @Override
    public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {}

    @Override
    public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {}

    @Override
    public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {}

    @Override
    public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {}

    @Override
    public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {}

    @Override
    public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x-4, y-3, x+w+4, y+h, getBackgroundColor());
    }

    @Override
    public int UITextColor() {
        return getFontColor();
    }

    @Override
    public int UIAltTextColor() {
        return getFontColor();
    }

    @Override
    public int UIBGColor() {
        return Colors.BLACK;
    }

    @Override
    public int UIBGBorderColor() {
        return 0;
    }

    @Override
    public int buttonTextColor() {
        return getFontColor();
    }

    @Override
    public int buttonTextOverColor() {
        return getFontColor();
    }

    @Override
    public int buttonTextDisabledColor() {
        return Colors.ChatColors.GRAY;
    }

    @Override
    public int errorColor() {
        return Colors.RED;
    }

    @Override
    public int successColor() {
        return 0xFFC1E52F;
    }

    @Override
    public boolean isUITextShadow() {
        return false;
    }

    @Override
    public boolean isButtonTextShadow() {
        return true;
    }
}
