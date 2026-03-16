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

import com.teammoeg.chorda.client.icon.FlatIcon;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 带操作状态反馈的图标按钮。点击后会切换显示的图标和文字信息，鼠标移开后恢复原始状态。
 * <p>
 * Action state icon button. Switches the displayed icon and message after being clicked,
 * and reverts to the original state when the mouse moves away.
 */
public class ActionStateIconButton extends IconButton {
    /** 原始消息文本 / Original message text */
    private final Component originalMessage;
    /** 原始图标 / Original icon */
    private final FlatIcon originalIcon;
    /** 点击后显示的消息文本 / Message text displayed after clicking */
    @Getter
    @Setter
    private Component clickedMessage;
    /** 点击后显示的图标 / Icon displayed after clicking */
    @Getter
    @Setter
    private FlatIcon clickedIcon;

    /**
     * 创建一个带操作状态反馈的图标按钮，使用默认缩放比例1。
     * <p>
     * Creates an action state icon button with default scale of 1.
     *
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param icon 默认图标 / Default icon
     * @param color 图标颜色 / Icon color
     * @param message 默认消息 / Default message
     * @param clickedMessage 点击后的消息 / Message after clicking
     * @param pressedAction 按下时的回调 / Callback when pressed
     */
    public ActionStateIconButton(int x, int y, FlatIcon icon, int color, Component message, Component clickedMessage, OnPress pressedAction) {
        this(x, y, icon, color, 1, message, clickedMessage, pressedAction);
    }

    /**
     * 创建一个带操作状态反馈的图标按钮，点击前后使用相同图标。
     * <p>
     * Creates an action state icon button that uses the same icon before and after clicking.
     *
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param icon 默认图标 / Default icon
     * @param color 图标颜色 / Icon color
     * @param scale 缩放比例 / Scale factor
     * @param message 默认消息 / Default message
     * @param clickedMessage 点击后的消息 / Message after clicking
     * @param pressedAction 按下时的回调 / Callback when pressed
     */
    public ActionStateIconButton(int x, int y, FlatIcon icon, int color, int scale, Component message, Component clickedMessage, OnPress pressedAction) {
        this(x, y, icon, icon, color, scale, message, clickedMessage, pressedAction);
    }

    /**
     * 创建一个带操作状态反馈的图标按钮，可分别指定点击前后的图标。
     * <p>
     * Creates an action state icon button with separate icons for before and after clicking.
     *
     * @param x x坐标 / X coordinate
     * @param y y坐标 / Y coordinate
     * @param icon 默认图标 / Default icon
     * @param clickedIcon 点击后的图标 / Icon after clicking
     * @param color 图标颜色 / Icon color
     * @param scale 缩放比例 / Scale factor
     * @param message 默认消息 / Default message
     * @param clickedMessage 点击后的消息 / Message after clicking
     * @param pressedAction 按下时的回调 / Callback when pressed
     */
    public ActionStateIconButton(int x, int y, FlatIcon icon, FlatIcon clickedIcon, int color, int scale, Component message, Component clickedMessage, OnPress pressedAction) {
        super(x, y, icon, color, scale, message, pressedAction);
        this.originalMessage = message;
        this.clickedMessage = clickedMessage;
        this.originalIcon = icon;
        this.clickedIcon = clickedIcon;
    }

    /**
     * 渲染按钮控件。当鼠标未悬停时，恢复为原始图标和消息。
     * <p>
     * Renders the button widget. Reverts to the original icon and message when the mouse is not hovering.
     *
     * @param graphics 图形上下文 / Graphics context
     * @param mouseX 鼠标X坐标 / Mouse X coordinate
     * @param mouseY 鼠标Y坐标 / Mouse Y coordinate
     * @param partialTicks 渲染插值时间 / Partial tick time for rendering interpolation
     */
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        if (!isHovered()) {
            setMessage(originalMessage);
            setIcon(originalIcon);
        }
    }

    /**
     * 处理点击事件。切换为点击后的图标和消息。
     * <p>
     * Handles click event. Switches to the clicked icon and message.
     *
     * @param pMouseX 鼠标X坐标 / Mouse X coordinate
     * @param pMouseY 鼠标Y坐标 / Mouse Y coordinate
     */
    @Override
    public void onClick(double pMouseX, double pMouseY) {
        super.onClick(pMouseX, pMouseY);
        setMessage(clickedMessage);
        setIcon(clickedIcon);
    }
}
