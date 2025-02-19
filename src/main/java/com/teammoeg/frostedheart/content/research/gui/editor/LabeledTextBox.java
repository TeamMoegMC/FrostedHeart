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

package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.cui.TextBox;
import com.teammoeg.chorda.client.cui.UIElement;



public class LabeledTextBox extends LabeledPane<TextBox> {
    String orig;

    public LabeledTextBox(UIElement panel, String lab, String txt) {
        super(panel, lab);
        obj = new TextBox(this);
        obj.allowInput();
        if (txt == null) txt = "";
        obj.setText(txt);
        obj.setSize(200, 16);
        orig = txt;


    }

    public String getText() {
        return obj.getText();
    }

    public void setText(String s) {
        obj.setText(s);
    }
}
