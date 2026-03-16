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

package com.teammoeg.chorda.client.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.math.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * 颜色编辑框控件。允许用户以十六进制格式输入颜色值，并在右侧显示颜色预览方块。
 * 支持带Alpha通道和不带Alpha通道两种模式。输入无效十六进制值时文字变红提示。
 * <p>
 * Color edit box widget. Allows users to input color values in hexadecimal format,
 * with a color preview square displayed to the right. Supports both with-alpha and
 * without-alpha modes. Text turns red when an invalid hex value is entered.
 */
public class ColorEditbox extends EditBox {
    /** 不带Alpha通道的前缀 / Prefix without alpha channel */
    private static final String PREFIX = "0x";
    /** 带Alpha通道的前缀 / Prefix with alpha channel */
    private static final String PREFIX_WITH_ALPHA = "0xFF";
    /** 字体渲染器 / Font renderer */
    protected final Font font;
    /** 是否使用Alpha通道模式 / Whether alpha channel mode is used */
    protected final boolean withAlpha;

    /**
     * 创建一个默认的颜色编辑框，启用Alpha通道模式，初始颜色为0。
     * <p>
     * Creates a default color edit box with alpha channel mode enabled and initial color of 0.
     *
     * @param font 字体渲染器 / Font renderer
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param width 宽度 / Width
     * @param height 高度 / Height
     * @param message 消息文本 / Message text
     */
    public ColorEditbox(Font font, int x, int y, int width, int height, Component message) {
        this(font, x, y, width, height, message, true, 0);
    }

    /**
     * 创建一个颜色编辑框，可指定Alpha通道模式和初始颜色值。
     * 当输入非法十六进制字符时，文本颜色变为红色。
     * <p>
     * Creates a color edit box with configurable alpha channel mode and initial color value.
     * Text color turns red when invalid hexadecimal characters are entered.
     *
     * @param font 字体渲染器 / Font renderer
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param width 宽度 / Width
     * @param height 高度 / Height
     * @param message 消息文本 / Message text
     * @param withAlpha 是否启用Alpha通道模式 / Whether to enable alpha channel mode
     * @param colorValue 初始颜色值 / Initial color value
     */
    public ColorEditbox(Font font, int x, int y, int width, int height, Component message, boolean withAlpha, int colorValue) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.withAlpha = withAlpha;
        setValue(colorValue);
        setMaxLength(withAlpha ? 6 : 8);
        setResponder(s -> {
            try {
                setTextColor(Colors.WHITE);
                Integer.parseUnsignedInt(s, 16);
            } catch (NumberFormatException e) {
                setTextColor(Colors.RED);
            }
        });
    }

    /**
     * 渲染颜色编辑框。在左侧绘制十六进制前缀，在右侧绘制带阴影的颜色预览方块。
     * <p>
     * Renders the color edit box. Draws the hex prefix on the left and a color preview
     * square with shadow on the right.
     *
     * @param graphics 图形上下文 / Graphics context
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param partialTick 渲染插值时间 / Partial tick time for rendering interpolation
     */
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, getPrefix(), getX()-2-font.width(getPrefix()), getY()+(getHeight()/2)-4, 0xFFFFFFFF);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(1, 1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), Colors.makeDark(getColorValue(), 0.75F));
        pose.translate(-1, -1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), getColorValue());
        pose.popPose();
    }

    /**
     * 获取当前输入的颜色整数值。如果输入无效则返回红色。
     * <p>
     * Gets the current color integer value from the input. Returns red if the input is invalid.
     *
     * @return 解析后的颜色值，无效输入返回红色 / Parsed color value, or red for invalid input
     */
    public int getColorValue() {
        try {
            return withAlpha ? Colors.setAlpha(Integer.parseUnsignedInt(getValue(), 16), 1F) : Integer.parseUnsignedInt(getValue(), 16);
        } catch (NumberFormatException e) {
            return Colors.RED;
        }
    }

    /**
     * 以整数形式设置颜色值，自动转换为十六进制字符串显示。
     * <p>
     * Sets the color value as an integer, automatically converting it to a hex string for display.
     *
     * @param value 颜色整数值 / Color integer value
     */
    public void setValue(int value) {
        if (withAlpha) {
            setValue(Colors.toHexString(value).substring(2).toUpperCase());
        } else {
            setValue(Colors.toHexString(value).toUpperCase());
        }
    }

    /**
     * 获取当前模式对应的十六进制前缀字符串。
     * <p>
     * Gets the hex prefix string for the current mode.
     *
     * @return 前缀字符串，带Alpha为"0xFF"，不带为"0x" / Prefix string, "0xFF" with alpha, "0x" without
     */
    private String getPrefix() {
        return withAlpha ? PREFIX_WITH_ALPHA : PREFIX;
    }

    /**
     * 获取完整的十六进制颜色值字符串。带Alpha模式下会自动补充"FF"前缀。
     * <p>
     * Gets the full hex color value string. In alpha mode, "FF" prefix is automatically prepended.
     *
     * @return 完整的十六进制颜色字符串 / Full hex color string
     */
    @Override
    public String getValue() {
        return (withAlpha ? "FF" : "") + super.getValue();
    }
}
