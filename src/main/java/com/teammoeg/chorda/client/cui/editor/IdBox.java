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

public class IdBox extends LabeledTextBoxAndBtn {
	String oldVal;
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
