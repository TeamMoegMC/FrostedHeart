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
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.text.Components;

import net.minecraft.network.chat.Component;

public class EditPrompt extends BaseEditDialog {
    LabeledTextBox box;
    Button ok;
    Button cancel;
    public EditPrompt(UIElement panel, Component label, String val, Consumer<String> onFinished,Verifier<String> verifier) {
        super(panel);
        box = new LabeledTextBox(this, label, val,verifier);
        ok = new TextButton(this, Components.translatable("gui.accept"), CIcons.nop()) {

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
        cancel = new TextButton(this, Components.translatable("gui.cancel"), CIcons.nop()) {

            @Override
            public void onClicked(MouseButton arg0) {
                close();
            }

        };
        cancel.setSize(300, 20);
        ok.setSize(300, 20);
    }

    public static void open(UIElement p, Component l, String v, Consumer<String> f) {
        new EditPrompt(p, l, v, f,null).open();
    }
    public static void open(UIElement p, Component l, String v, Consumer<String> f,Verifier<String> verif) {
        new EditPrompt(p, l, v, f,verif).open();
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
