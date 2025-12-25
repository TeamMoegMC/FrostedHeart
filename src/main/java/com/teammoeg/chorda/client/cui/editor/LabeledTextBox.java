/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.chorda.client.cui.TextBox;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.cui.editor.Verifier.VerifyResult;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;



public class LabeledTextBox extends LabeledPane<TextBox> {
    String orig;
    VerifyResult result;
    public LabeledTextBox(UIWidget panel, Component lab, String txt) {
    	this(panel,lab,txt,null);
    }
    public LabeledTextBox(UIWidget panel, Component lab, String txt,Verifier<String> verif) {
        super(panel, lab);
        obj = new TextBox(this);
        obj.allowInput();
        if(verif!=null)
        obj.setFilter(verif);
        if (txt == null) txt = "";
        obj.setText(txt);
        obj.setSize(200, 16);
        orig = txt;
    }

    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
		super.render(graphics, x, y, w, h);
	}

	public String getText() {
		if(obj.isTextValid())
			return obj.getText();
		return orig;
    }

    public void setText(String s) {
        obj.setText(s);
    }
}
