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

import blusunrize.immersiveengineering.client.ClientUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.util.client.PositionedIngredient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.Optional;

public abstract class TechTextButton extends TechButton {

    public TechTextButton(Panel panel, Component txt, Icon icon) {
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
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        //drawBackground(matrixStack, theme, x, y, w, h);
        TechIcons.drawTexturedRect(matrixStack, x, y, w, h, isMouseOver());
        int s = h >= 16 ? 16 : 8;
        int off = (h - s) / 2;
        FormattedText title = getTitle();
        int textX = x;
        int textY = y + (h - theme.getFontHeight() + 1) / 2;
        int sw = ClientUtils.mc().font.width(title);
        int mw = w - (hasIcon() ? off + s : 0) - 6;

        if (sw > mw) {
            sw = mw;
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
        //RenderSystem.setShaderColor(textY, sw, mw, h);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        //FIXME: IDK WHY BUT SHADER SEEMS NULL WHEN RENDERING ANYTHING.
        //System.out.println(RenderSystem.getShader());
        matrixStack.drawWordWrap(ClientUtils.mc().font, title, textX, textY, mw, TechIcons.text.rgba());
    }

    @Override
    public Optional<PositionedIngredient> getIngredientUnderMouse() {
        Object igd = icon.getIngredient();
        return igd instanceof PositionedIngredient ? Optional.of((PositionedIngredient) igd) : Optional.empty();
    }


    public boolean renderTitleInCenter() {
        return false;
    }

    @Override
    public TechTextButton setTitle(Component txt) {
        super.setTitle(txt);
        setWidth(getGui().getTheme().getStringWidth(getTitle()) + (hasIcon() ? 28 : 8));
        return this;
    }

}
