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

package com.teammoeg.chorda.client.ui;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * GUI点击事件监听器。定义一个矩形区域，当鼠标点击该区域时执行指定的回调。
 * <p>
 * GUI click event listener. Defines a rectangular region that executes a callback when clicked.
 */
public class GuiClickedEvent implements GuiEventListener {
    int x1;
    int y1;
    int x2;
    int y2;
    Runnable call;
    boolean focused;

    /**
     * 构造一个GUI点击事件。
     * <p>
     * Constructs a GUI click event.
     *
     * @param x1 矩形区域左上角X坐标 / X coordinate of the top-left corner
     * @param y1 矩形区域左上角Y坐标 / Y coordinate of the top-left corner
     * @param x2 矩形区域右下角X坐标 / X coordinate of the bottom-right corner
     * @param y2 矩形区域右下角Y坐标 / Y coordinate of the bottom-right corner
     * @param call 点击时执行的回调 / Callback to execute on click
     */
    public GuiClickedEvent(int x1, int y1, int x2, int y2, Runnable call) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.call = call;
    }

    /**
     * 处理鼠标点击事件。如果点击位置在定义的矩形区域内，则执行回调。
     * <p>
     * Handles mouse click events. Executes the callback if the click is within the defined rectangular region.
     *
     * @param mx 鼠标X坐标 / Mouse X coordinate
     * @param my 鼠标Y坐标 / Mouse Y coordinate
     * @param button 鼠标按键 / Mouse button
     * @return 如果点击在区域内则返回true / True if the click was within the region
     */
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (x1 <= mx && mx <= x2 && y1 <= my && my <= y2) {
            call.run();
            return true;
        }
        return false;
    }

	/** {@inheritDoc} */
	@Override
	public void setFocused(boolean pFocused) {
		focused=pFocused;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isFocused() {
		return focused;
	}

}
