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

package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class EditBtnDialog extends BaseEditDialog {
    public static final Editor<String> EDITOR_ITEM_TAGS = (p, l, v, c) -> {


        new EditBtnDialog(p, l, v, "Select Tag", c, SelectDialog.EDITOR_ITEM_TAGS).open();
    };
    LabeledTextBoxAndBtn box;
    Button ok;
    Button cancel;

    public EditBtnDialog(Widget panel, String label, String val, String sel, Consumer<String> onFinished, Editor<String> onbtn) {
        super(panel);
        box = new LabeledTextBoxAndBtn(this, label, val, sel, e -> onbtn.open(panel, sel, box.getText(), e));
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

    public EditBtnDialog(Widget panel, String label, String val, String sel, Consumer<String> onFinished, Function<String, String> onbtn) {
        this(panel, label, val, sel, onFinished, (p, l, v, c) -> c.accept(onbtn.apply(v)));

    }


    @Override
    public void onClose() {
    }

    @Override
    public void addWidgets() {

        add(box);
        add(ok);
        add(cancel);
    }
}
