/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class EditPrompt extends BaseEditDialog {
    public static Editor<String> TEXT_EDITOR = EditPrompt::open;
    public static Editor<JsonElement> JSON_EDITOR = (p, l, v, c) -> {
        open(p, l, v == null ? "" : v.toString(), e -> c.accept(new JsonParser().parse(e)));
    };
    public static Editor<Long> LONG_EDITOR = (p, l, v, c) -> {

        open(p, l, String.valueOf(v), o -> {
            c.accept(Long.parseLong(o));
        });
    };
    public static Editor<Integer> INT_EDITOR = (p, l, v, c) -> {
        open(p, l, String.valueOf(v), o -> {
            c.accept(Integer.parseInt(o));
        });
    };
    public static Editor<Double> REAL_EDITOR = (p, l, v, c) -> {
        open(p, l, String.valueOf(v), o -> {
            c.accept(Double.parseDouble(o));
        });
    };
    LabeledTextBox box;
    Button ok;
    Button cancel;

    public static void open(Widget p, String l, String v, Consumer<String> f) {
        new EditPrompt(p, l, v, f).open();
    }

    public EditPrompt(Widget panel, String label, String val, Consumer<String> onFinished) {
        super(panel);
        box = new LabeledTextBox(this, label, val);
        ok = new SimpleTextButton(this, GuiUtils.str("OK"), Icon.EMPTY) {

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
        cancel = new SimpleTextButton(this, GuiUtils.str("Cancel"), Icon.EMPTY) {

            @Override
            public void onClicked(MouseButton arg0) {
                close();
            }

        };
        cancel.setSize(300, 20);
        ok.setSize(300, 20);
    }

    @Override
    public void addWidgets() {

        add(box);
        add(ok);
        add(cancel);
    }

    @Override
    public void onClose() {
    }


}
