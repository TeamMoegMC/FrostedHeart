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

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 图标复选框控件。使用{@link FlatIcon}图标代替传统复选框样式。
 * 选中时显示对勾图标（CHECK），未选中时显示叉号图标（CROSS），颜色为青色。
 * <p>
 * Icon checkbox widget. Uses {@link FlatIcon} icons instead of the traditional checkbox style.
 * Displays a check icon (CHECK) when selected and a cross icon (CROSS) when unselected,
 * rendered in cyan color.
 */
public class IconCheckbox extends Checkbox {
    /** 选中状态的图标（对勾） / Icon for the selected state (check mark) */
    private static final FlatIcon SELECTED_ICON = FlatIcon.CHECK;
    /** 缩放比例 / Scale factor */
    private int scale;

    /**
     * 创建一个图标复选框，使用默认缩放比例1。
     * <p>
     * Creates an icon checkbox with default scale of 1.
     *
     * @param pX x坐标 / X coordinate
     * @param pY y坐标 / Y coordinate
     * @param pMessage 复选框文本 / Checkbox message text
     * @param pSelected 初始选中状态 / Initial selected state
     */
    public IconCheckbox(int pX, int pY, Component pMessage, boolean pSelected) {
        this(pX, pY, 1, pMessage, pSelected);
    }

    /**
     * 创建一个图标复选框，可指定缩放比例。
     * <p>
     * Creates an icon checkbox with configurable scale.
     *
     * @param pX x坐标 / X coordinate
     * @param pY y坐标 / Y coordinate
     * @param scale 缩放比例，最小为1 / Scale factor, minimum 1
     * @param pMessage 复选框文本 / Checkbox message text
     * @param pSelected 初始选中状态 / Initial selected state
     */
    public IconCheckbox(int pX, int pY, int scale, Component pMessage, boolean pSelected) {
        super(pX, pY, SELECTED_ICON.size.width, SELECTED_ICON.size.height, pMessage, pSelected, false);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
    }

    /**
     * 设置缩放比例，并根据新的缩放值重新计算控件尺寸。
     * <p>
     * Sets the scale factor and recalculates the widget dimensions based on the new scale.
     *
     * @param scale 新的缩放比例 / New scale factor
     */
    public void setScale(int scale) {
        int w = this.width / this.scale;
        int h = this.height / this.scale;
        this.scale = scale;
        this.width = w * scale;
        this.height = h * scale;
    }

    /**
     * 渲染图标复选框。根据选中状态显示对勾或叉号图标，使用青色着色。
     * <p>
     * Renders the icon checkbox. Displays check or cross icon based on the selected state,
     * rendered with cyan coloring.
     *
     * @param pGuiGraphics 图形上下文 / Graphics context
     * @param pMouseX 鼠标X坐标 / Mouse X coordinate
     * @param pMouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param pPartialTick 渲染插值时间 / Partial tick time for rendering interpolation
     */
    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        FlatIcon icon = selected() ? SELECTED_ICON : FlatIcon.CROSS;
        RenderSystem.enableDepthTest();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        CGuiHelper.bindTexture(FlatIcon.ICON_LOCATION);
        CGuiHelper.blitColored(pGuiGraphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), IconButton.TEXTURE_WIDTH*scale, IconButton.TEXTURE_HEIGHT*scale, Colors.CYAN, alpha);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


}
