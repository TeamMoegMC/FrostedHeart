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

import com.teammoeg.chorda.client.StringTextComponentParser;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.text.Components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/**
 * 限宽文本字段控件。在指定宽度内显示文本，超出部分用省略号截断。
 * 当文本被截断时，可通过悬停提示显示完整文本。
 * <p>
 * Limited-width text field widget. Displays text within a specified width,
 * truncating with ellipsis when overflowing. When text is truncated, the full
 * text can be shown via hover tooltip.
 */
public class LimitedTextField extends UIElement {
    /** 原始标题文本 / Original title text */
    private Component title;
    /** 经过截断处理的显示文本 / Display text after truncation processing */
    private Component displayTitle = Components.immutableEmpty();
    /** 文本颜色 / Text color */
    public int color;
    /** 是否启用提示框 / Whether tooltip is enabled */
    public boolean tooltip = true;

    /**
     * 使用默认UI文本颜色创建限宽文本字段。
     * <p>
     * Creates a limited text field with the default UI text color.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param title 标题文本 / Title text
     * @param w 最大显示宽度 / Maximum display width
     */
    public LimitedTextField(UIElement parent, Component title, int w) {
        this(parent, title, w, parent.theme().UITextColor());
    }

    /**
     * 使用指定文本颜色创建限宽文本字段。
     * <p>
     * Creates a limited text field with the specified text color.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param title 标题文本 / Title text
     * @param w 最大显示宽度 / Maximum display width
     * @param textColor 文本颜色 / Text color
     */
    public LimitedTextField(UIElement parent, Component title, int w, int textColor) {
        super(parent);
        this.title = title;
        this.color = textColor;
        setSize(w, getFont().lineHeight);
    }

    /** {@inheritDoc} */
    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.drawString(getFont(), displayTitle, x, y, color, theme().isButtonTextShadow());
    }

    /**
     * 刷新显示文本。如果文本超出宽度限制，则截断并添加省略号。
     * <p>
     * Refreshes the display text. Truncates and appends ellipsis if text exceeds width limit.
     */
    @Override
    public void refresh() {
        final int space = 2;
        if (getFont().width(title) > getWidth()-space) {
            var e = CommonComponents.ELLIPSIS;
            var sub = getFont().substrByWidth(title, getWidth()-space - getFont().width(e));
            sub = FormattedText.composite(sub, e);
            displayTitle = StringTextComponentParser.parse(sub.getString()).withStyle(title.getStyle());
            return;
        }
        displayTitle = StringTextComponentParser.parse(title.getString());
    }

    /**
     * 设置是否在文本被截断时显示提示框。
     * <p>
     * Sets whether to show a tooltip when text is truncated.
     *
     * @param b 是否显示提示框 / Whether to show tooltip
     * @return 当前实例（链式调用） / This instance (for chaining)
     */
    public LimitedTextField shouldShowTooltip(boolean b) {
        tooltip = b;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasTooltip() {
        return (tooltip && getFont().width(title) > getWidth()) && super.hasTooltip();
    }

    /** {@inheritDoc} */
    @Override
    public Component getTitle() {
        return this.title;
    }

    /**
     * 设置标题文本并刷新显示。
     * <p>
     * Sets the title text and refreshes the display.
     *
     * @param title 新标题文本 / New title text
     */
    public void setTitle(Component title) {
        this.title = title;
        refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void setWidth(int v) {
        super.setWidth(v);
        refresh();
    }
}
