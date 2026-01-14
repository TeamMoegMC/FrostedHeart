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

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public abstract class TechTextButton extends TechButton {

    public TechTextButton(UIElement panel, Component txt, CIcon icon) {
        super(panel, txt, icon);
        setWidth(getFont().width(txt) + (hasIcon() ? 28 : 8));
        setHeight(20);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        if (getFont().width(getTitle()) + (hasIcon() ? 28 : 8) > width) {
            list.accept(getTitle());
        }
    }

    @Override
    public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
        //drawBackground(matrixStack, theme, x, y, w, h);
        TechIcons.drawTexturedRect(matrixStack, x, y, w, h, isMouseOver());
        int s = h >= 16 ? 16 : 8;
        int off = (h - s) / 2;
        FormattedText title = getTitle();
        int textX = x;
        int textY = y + (h - getFont().lineHeight + 1) / 2;
        int sw = ClientUtils.getMc().font.width(title);
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
            drawIcon(matrixStack, x + off, y + off, s, s);
            textX += off + s;
        }
        //RenderSystem.setShaderColor(textY, sw, mw, h);
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        //System.out.println(RenderSystem.getShader());
        matrixStack.drawWordWrap(ClientUtils.font(), title, textX, textY, mw, TechIcons.text);
    }

    /*@Override
    public Optional<PositionedIngredient> getIngredientUnderMouse() {
        Object igd = icon.getIngredient();
        return igd instanceof PositionedIngredient ? Optional.of((PositionedIngredient) igd) : Optional.empty();
    }*/


    public boolean renderTitleInCenter() {
        return false;
    }

    @Override
    public TechTextButton setTitle(Component txt) {
        super.setTitle(txt);
        setWidth(getFont().width(getTitle()) + (hasIcon() ? 28 : 8));
        return this;
    }

}
