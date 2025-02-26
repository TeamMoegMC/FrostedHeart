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

import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.network.chat.Component;

public class LabeledOpenEditorButton<T> extends LabeledPane<OpenEditorButton<T>> {

    public LabeledOpenEditorButton(UIWidget panel, Component master, Component label, Editor<T> e, T val, Consumer<T> cb) {
        super(panel, master);
        obj = new OpenEditorButton<>(this, label, e, val, cb);
    }

    public LabeledOpenEditorButton(UIWidget panel, Component master, Component label, Editor<T> e, T val, CIcon ic, Consumer<T> cb) {
        super(panel, master);
        obj = new OpenEditorButton<>(this, label, e, val, ic, cb);
    }
    public LabeledOpenEditorButton(UIWidget panel, Component master, Editor<T> edi, T val, Function<T, CIcon> getIcon, Function<T, Component> getText) {
        super(panel, master);
        obj = new OpenEditorButton<>(this, master, edi, val, getIcon, getText);
    }
    T getValue() {
    	return obj.getValue();
    }

	public void setValue(T v) {
		obj.setValue(v);
		
	}
}
