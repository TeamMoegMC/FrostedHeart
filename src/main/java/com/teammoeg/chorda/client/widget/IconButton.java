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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.TesselateHelper.ShapeTesslator;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 图标按钮控件。使用{@link FlatIcon}作为视觉表示的可点击按钮。
 * 支持自定义颜色、缩放，悬停时在按钮上方显示消息文本并添加半透明背景高亮。
 * 非激活状态下图标显示为灰色。
 * <p>
 * Icon button widget. A clickable button using {@link FlatIcon} as visual representation.
 * Supports custom color and scaling. Shows message text above the button with a
 * semi-transparent background highlight on hover. Displays the icon in gray when inactive.
 */
public class IconButton extends Button {
    /** 按钮显示的图标 / The icon displayed on the button */
    @Setter
    private FlatIcon icon;
    /** 图标颜色 / Icon color */
    public int color;
    // 为什么是int? 混素达咩(
    /** 缩放比例 / Scale factor */
    private int scale;

    /**
     * 创建一个图标按钮，使用默认缩放比例1。
     * <p>
     * Creates an icon button with default scale of 1.
     *
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param icon 按钮的图标 {@link FlatIcon} / The button icon
     * @param color 图标颜色 / Icon color
     * @param title 按钮标题文本 / Button title text
     * @param pressedAction 按下时的回调 / Callback when pressed
     */
    public IconButton(int x, int y, FlatIcon icon, int color, Component title, OnPress pressedAction) {
        this(x, y, icon, color, 1, title, pressedAction);
    }

    /**
     * 创建一个图标按钮，可指定缩放比例。按钮尺寸根据图标大小和缩放比例计算。
     * <p>
     * Creates an icon button with configurable scale. Button dimensions are calculated
     * from the icon size and scale factor.
     *
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param icon 按钮的图标 {@link FlatIcon} / The button icon
     * @param color 图标颜色 / Icon color
     * @param scale 缩放比例，最小为1 / Scale factor, minimum 1
     * @param title 按钮标题文本 / Button title text
     * @param pressedAction 按下时的回调 / Callback when pressed
     */
    public IconButton(int x, int y, FlatIcon icon, int color, int scale, Component title, OnPress pressedAction) {
        super(x, y, icon.size.width, icon.size.height, title, pressedAction, Button.DEFAULT_NARRATION);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
        this.color = color;
        this.icon = icon;
    }

    /**
     * 渲染图标按钮。处理聚焦边框、悬停高亮、消息文本和带颜色的图标绘制。
     * 悬停时在按钮上方显示消息文本，文本位置会根据可用空间自动调整。
     * <p>
     * Renders the icon button. Handles focus border, hover highlight, message text,
     * and colored icon drawing. When hovered, displays message text above the button,
     * with text position automatically adjusted based on available space.
     *
     * @param graphics 图形上下文 / Graphics context
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param partialTicks 渲染插值时间 / Partial tick time for rendering interpolation
     */
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int color = isActive() ? this.color : 0xFF666666;
        int backgroundColor = Colors.makeDark(color, 0.3F);
        float alpha = 0.5F;
        int fontX=Integer.MIN_VALUE;
        
        try(ShapeTesslator tesselator=TesselateHelper.getShapeTesslator()){
	        if (isFocused()) {
	        	tesselator.drawRectWH(graphics.pose().last().pose(), getX(), getY(), getWidth(), getHeight(), color, false);
	        }
	        if (isHovered()) {
	        	tesselator.fillRect(graphics.pose().last().pose(), getX(), getY(), getX()+getWidth(), getY()+getHeight(), Colors.setAlpha(backgroundColor, alpha));
	            if (!getMessage().getString().isBlank() && isHovered()) {
	                int textWidth = ClientUtils.font().width(getMessage());
	                int renderX = getX()-textWidth+8;
	                if (renderX < 0) {
	                	tesselator.fillRect(graphics.pose().last().pose(), 
	                			getX(),
	                            getY()-12,
	                            getX()+2 + textWidth,
	                            getY(),
	                            Colors.setAlpha(backgroundColor, alpha));
	                	fontX=getX()+2;
	                } else {
	                	tesselator.fillRect(graphics.pose().last().pose(), 
	                		getX()-textWidth+getWidth()-1,
	                            getY()-12,
	                            getX()+getWidth(),
	                            getY(),
	                            Colors.setAlpha(backgroundColor, alpha));
	                	fontX=getX()-textWidth+getWidth();
	                }
	            }
	        }
        }
        if(fontX!=Integer.MIN_VALUE)
        	graphics.drawString(ClientUtils.font(), getMessage(), fontX, getY()-10, color);
        CGuiHelper.bindTexture(FlatIcon.ICON_LOCATION);
        CGuiHelper.blitColored(graphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), FlatIcon.TEXTURE_WIDTH*scale, FlatIcon.TEXTURE_HEIGHT*scale, color, this.alpha);
    }

    /**
     * 设置缩放比例，并根据新的缩放值重新计算按钮尺寸。
     * <p>
     * Sets the scale factor and recalculates the button dimensions based on the new scale.
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
}
