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


import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;

import net.minecraft.network.chat.Component;

public class LabeledTextBoxAndBtn extends LabeledTextBox {
    Button btn;

    public LabeledTextBoxAndBtn(UIElement panel, Component lab, String txt, Component btn, Consumer<Consumer<String>> onbtn) {
        super(panel, lab, txt);
        this.btn = new TextButton(this, btn, CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                onbtn.accept(s -> obj.setText(s));
            }
        };
    }

    @Override
    public void addUIElements() {
        super.addUIElements();
        add(btn);
    }
}
