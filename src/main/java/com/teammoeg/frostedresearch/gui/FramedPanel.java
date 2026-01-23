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

import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class FramedPanel extends UILayer {
    Component title;
    Consumer<UILayer> addWidgets;

    public FramedPanel(UIElement panel, Consumer<UILayer> addWidgets) {
        super(panel);
        this.addWidgets = addWidgets;
    }

    @Override
    public void addUIElements() {
        if (addWidgets != null)
            addWidgets.accept(this);
        for (UIElement w : super.elements) {
            w.setPos(w.getX() + 5, w.getY() + 12);
            w.setWidth(Math.min(w.getWidth(), width - 12));
        }
    }

    @Override
    public void alignWidgets() {
        setHeight(height + 12);

    }

    @Override
    public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
    	matrixStack.drawString(getFont(), title, x, y, TechIcons.text, false);
    	DrawDeskTheme.drawPanel(matrixStack, x, y, w, h);

        super.render(matrixStack, x, y, w, h);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {

    }

    public void setTitle(Component title) {
        this.title = title;
    }

}
