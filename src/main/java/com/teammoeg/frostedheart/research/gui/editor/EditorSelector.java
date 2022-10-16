/*
 * Copyright (c) 2022 TeamMoeg
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

import dev.ftb.mods.ftblibrary.ui.Widget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class EditorSelector<T> extends BaseEditDialog {
    private final Map<String, Editor<? extends T>> editors = new LinkedHashMap<>();
    private final String label;
    private final T val;
    private final BiPredicate<T, String> getEditor;
    private final Consumer<T> callback;


    public EditorSelector(Widget panel, String label, BiPredicate<T, String> pred, T val, Consumer<T> callback) {
        super(panel);
        this.label = label;
        this.val = val;
        this.getEditor = pred;
        this.callback = callback;
    }

    public EditorSelector(Widget panel, String label, Consumer<T> callback) {
        this(panel, label, (o, s) -> false, null, callback);
    }

    public EditorSelector<T> addEditor(String name, Editor<? extends T> e) {
        editors.put(name, e);
        return this;
    }

    @Override
    public void onClose() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addWidgets() {
        add(EditUtils.getTitle(this, label));
        for (Entry<String, Editor<? extends T>> ent : editors.entrySet()) {
            add(new OpenEditorButton<>(this, ent.getKey(), (Editor<T>) ent.getValue().and((p, l, v, c) -> close(false)), (val != null && getEditor.test(val, ent.getKey()) ? val : null), e -> callback.accept(e)));
        }
    }

}
