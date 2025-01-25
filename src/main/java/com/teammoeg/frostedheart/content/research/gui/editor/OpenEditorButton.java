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

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

import java.util.function.Consumer;

import com.teammoeg.chorda.lang.Components;

public class OpenEditorButton<T> extends SimpleTextButton {
    private final String lbl;
    private final Editor<T> edi;
    private final T val;
    private final Consumer<T> cb;

    public OpenEditorButton(Panel panel, String label, Editor<T> e, T val, Consumer<T> cb) {
        this(panel, label, e, val, Icon.empty(), cb);
    }

    public OpenEditorButton(Panel panel, String label, Editor<T> e, T val, Icon ic, Consumer<T> cb) {
        super(panel, Components.str(label), ic);
        lbl = label;
        edi = e;
        this.val = val;
        this.cb = cb;
    }

    @Override
    public void onClicked(MouseButton arg0) {
        edi.open(this, lbl, val, cb);
    }


}
