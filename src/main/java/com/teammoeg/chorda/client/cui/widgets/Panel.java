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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 面板控件。带标题的UI图层容器，使用主题绘制面板边框，子元素自动偏移以留出标题和边距空间。
 * <p>
 * Panel widget. A titled UI layer container that draws panel borders using the theme,
 * with child elements automatically offset to leave space for the title and margins.
 */
public class Panel extends UILayer {
    /** 面板标题 / Panel title */
    Component title;
    /** 添加子控件的回调 / Callback for adding child widgets */
    Consumer<UILayer> addWidgets;

    /**
     * 创建面板控件。
     * <p>
     * Creates a panel widget.
     *
     * @param panel 父级UI元素 / Parent UI element
     * @param addWidgets 添加子控件的回调函数 / Callback function for adding child widgets
     */
    public Panel(UIElement panel, Consumer<UILayer> addWidgets) {
        super(panel);
        this.addWidgets = addWidgets;
    }

    /**
     * 添加UI子元素并自动调整位置偏移，为标题和边距留出空间。
     * <p>
     * Adds child UI elements and auto-adjusts position offsets to leave space for title and margins.
     */
    @Override
    public void addUIElements() {
        if (addWidgets != null)
            addWidgets.accept(this);
        for (UIElement w : super.elements) {
            w.setPos(w.getX() + 5, w.getY() + 12);
            w.setWidth(Math.min(w.getWidth(), width - 12));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void alignWidgets() {
        setHeight(height + 12);

    }

    /** {@inheritDoc} */
    @Override
    public void render(GuiGraphics matrixStack, int x, int y, int w, int h, RenderingHint hint) {
    	matrixStack.drawString(getFont(), title, x, y, hint.theme(this).UITextColor(), hint.theme(this).isUITextShadow());
    	hint.theme(this).drawPanel(matrixStack, x, y, w, h);

        super.render(matrixStack, x, y, w, h, hint);
    }

    /** {@inheritDoc} */
    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h, RenderingHint hint) {

    }

    /**
     * 设置面板标题。
     * <p>
     * Sets the panel title.
     *
     * @param title 面板标题 / Panel title
     */
    public void setTitle(Component title) {
        this.title = title;
    }

}
