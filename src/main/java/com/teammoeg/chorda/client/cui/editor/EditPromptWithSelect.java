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

import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.network.chat.Component;

public class EditPromptWithSelect extends BaseEditDialog {
    LabeledTextBoxAndBtn box;
    Button ok;
    Button cancel;

    public EditPromptWithSelect(UIWidget panel, Component label, String val, Component sel, Consumer<String> onFinished, Editor<String> onbtn) {
        super(panel);
        box = new LabeledTextBoxAndBtn(this, label, val, sel, e -> onbtn.open(panel, sel, box.getText(), e));
        ok = new TextButton(this, Components.str("OK"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                try {
                    onFinished.accept(box.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                close();
            }

        };
        cancel = new TextButton(this, Components.str("Cancel"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                close();
            }

        };
        cancel.setSize(300, 20);
        ok.setSize(300, 20);
    }

    public EditPromptWithSelect(UIWidget panel, Component label, String val, Component sel, Consumer<String> onFinished, Function<String, String> onbtn) {
        this(panel, label, val, sel, onFinished, (p, l, v, c) -> c.accept(onbtn.apply(v)));

    }


    @Override
    public void addUIElements() {

        add(box);
        add(ok);
        add(cancel);
    }

    @Override
    public void onClose() {
    }
}
