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

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;

public class LabeledOpenEditorButton<T> extends LabeledPane<OpenEditorButton<T>> {

    public LabeledOpenEditorButton(Panel panel, String master, String label, Editor<T> e, T val, Consumer<T> cb) {
        super(panel, master);
        obj = new OpenEditorButton<T>(this, label, e, val, cb);
    }

    public LabeledOpenEditorButton(Panel panel, String master, String label, Editor<T> e, T val, Icon ic, Consumer<T> cb) {
        super(panel, master);
        obj = new OpenEditorButton<T>(this, label, e, val, ic, cb);
    }
}
