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
import com.teammoeg.chorda.client.cui.base.Verifiers;

import net.minecraft.network.chat.Component;

/**
 * 长整型数字输入框，继承自LabeledTextBox并使用数字验证器。
 * 提供数值的读取和设置方法，解析失败时返回原始值。
 * <p>
 * Long integer number input box extending LabeledTextBox with number validation.
 * Provides methods for reading and setting numeric values, falling back to the
 * original value on parse failure.
 */
public class NumberBox extends LabeledTextBox {

    public NumberBox(UIElement panel, Component lab, long val) {
        super(panel, lab, String.valueOf(val),Verifiers.NUMBER_STR);
    }

    public long getNum() {
        try {
            return Long.parseLong(getText());
        } catch (NumberFormatException ex) {
            Chorda.LOGGER.error("Error parsing number", ex);
            return Long.parseLong(orig);
        }

    }

    public NumberBox setNum(long number) {
        super.setText(String.valueOf(number));
        return this;
    }

}
