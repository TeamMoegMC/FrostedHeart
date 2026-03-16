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

import java.util.UUID;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;

/**
 * ID输入框，扩展带按钮的文本框，提供随机ID生成和重置到原始值功能。
 * 如果初始值为空，自动生成一个基于UUID的随机十六进制ID。
 * <p>
 * ID input box extending the text box with button, providing random ID generation
 * and reset-to-original-value functionality. If the initial value is empty,
 * automatically generates a random hexadecimal ID based on UUID.
 */
public class IdBox extends LabeledTextBoxAndBtn {
	String oldVal;
	/**
	 * 创建一个ID输入框。
	 * <p>
	 * Creates an ID input box.
	 *
	 * @param panel 父UI元素 / the parent UI element
	 * @param lab   标签文本 / the label text
	 * @param txt   初始ID值 / the initial ID value
	 */
	public IdBox(UIElement panel, Component lab, String txt) {
		super(panel, lab, txt, Components.str("Random"), s->s.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
		oldVal=txt;
		if(txt==null||txt.isEmpty())
			setText(Long.toHexString(UUID.randomUUID().getMostSignificantBits()));
	}
	String getOldValue() {
		return oldVal;
	}
	@Override
	public void addUIElements() {
		super.addUIElements();
		add(new TextButton(this, Components.str("Reset"), CIcons.nop()) {
			@Override
			public void onClicked(MouseButton button) {
				setText(oldVal);
			}
		});
	}
}
