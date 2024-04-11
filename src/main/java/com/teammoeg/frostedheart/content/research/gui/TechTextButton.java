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

package com.teammoeg.frostedheart.content.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;

public abstract class TechTextButton extends TechButton {

    public TechTextButton(Panel panel, ITextComponent txt, Icon icon) {
        super(panel, txt, icon);
        setWidth(panel.getGui().getTheme().getStringWidth(txt) + (hasIcon() ? 28 : 8));
        setHeight(20);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        if (getGui().getTheme().getStringWidth(getTitle()) + (hasIcon() ? 28 : 8) > width) {
            list.add(getTitle());
        }
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        drawBackground(matrixStack, theme, x, y, w, h);
        int s = h >= 16 ? 16 : 8;
        int off = (h - s) / 2;
        ITextProperties title = getTitle();
        int textX = x;
        int textY = y + (h - theme.getFontHeight() + 1) / 2;

        int sw = theme.getStringWidth(title);
        int mw = w - (hasIcon() ? off + s : 0) - 6;

        if (sw > mw) {
            sw = mw;
            title = theme.trimStringToWidth(title, mw);
        }

        if (renderTitleInCenter()) {
            textX += (mw - sw + 6) / 2;
        } else {
            textX += 4;
        }

        if (hasIcon()) {
            drawIcon(matrixStack, theme, x + off, y + off, s, s);
            textX += off + s;
        }

        theme.drawString(matrixStack, title, textX, textY, TechIcons.text, 0);
    }

    @Override
    public Object getIngredientUnderMouse() {
        return icon.getIngredient();
    }


    public boolean renderTitleInCenter() {
        return false;
    }

    @Override
    public TechTextButton setTitle(ITextComponent txt) {
        super.setTitle(txt);
        setWidth(getGui().getTheme().getStringWidth(getTitle()) + (hasIcon() ? 28 : 8));
        return this;
    }

}
