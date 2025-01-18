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

import com.teammoeg.chorda.util.lang.Components;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

import java.util.function.Consumer;

public class LabeledTextBoxAndBtn extends LabeledTextBox {
    Button btn;

    public LabeledTextBoxAndBtn(Panel panel, String lab, String txt, String btn, Consumer<Consumer<String>> onbtn) {
        super(panel, lab, txt);
        this.btn = new SimpleTextButton(this, Components.str(btn), Icon.empty()) {

            @Override
            public void onClicked(MouseButton arg0) {
                onbtn.accept(s -> obj.setText(s));
            }
        };
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        add(btn);
    }
}
