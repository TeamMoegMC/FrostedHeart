package com.teammoeg.frostedheart.content.research.gui;

import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.editor.LabeledTextBoxAndBtn;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public class IdBox extends LabeledTextBoxAndBtn {
	String oldVal;
	public IdBox(UIWidget panel, Component lab, String txt) {
		super(panel, lab, txt, Components.str("Random"), s->s.accept(Long.toHexString(UUID.randomUUID().getMostSignificantBits())));
		oldVal=txt;
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
