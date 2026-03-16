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

package com.teammoeg.chorda.client.cui.editor;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.base.UIElement;

import net.minecraft.network.chat.Component;

/**
 * 双精度浮点数输入框，继承自LabeledTextBox。
 * 提供浮点数值的读取和设置方法，解析失败时返回原始值。
 * <p>
 * Double-precision floating-point number input box extending LabeledTextBox.
 * Provides methods for reading and setting double values, falling back to the
 * original value on parse failure.
 */
public class RealBox extends LabeledTextBox {

    public RealBox(UIElement panel, Component lab, Double val) {
        super(panel, lab, val==null?"0":String.valueOf(val));
    }

    public double getNum() {
        try {
            return Double.parseDouble(getText());
        } catch (NumberFormatException ex) {
        	Chorda.LOGGER.error("Error parsing number", ex);
            return Double.parseDouble(orig);
        }
    }

    public void setNum(double number) {
        super.setText(String.valueOf(number));
    }

}
