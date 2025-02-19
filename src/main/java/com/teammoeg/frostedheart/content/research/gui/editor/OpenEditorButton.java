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


import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.lang.Components;

public class OpenEditorButton<T> extends TextButton {
    private final String lbl;
    private final Editor<T> edi;
    private final T val;
    private final Consumer<T> cb;

    public OpenEditorButton(UIElement panel, String label, Editor<T> e, T val, Consumer<T> cb) {
        this(panel, label, e, val, CIcons.nop(), cb);
    }

    public OpenEditorButton(UIElement panel, String label, Editor<T> e, T val, CIcon ic, Consumer<T> cb) {
        super(panel, Components.str(label), ic);
        lbl = label;
        edi = e;
        this.val = val;
        this.cb = cb;
    }

    @Override
    public void onClicked(MouseButton arg0) {
        edi.open(this.getParent(), lbl, val, cb);
    }


}
