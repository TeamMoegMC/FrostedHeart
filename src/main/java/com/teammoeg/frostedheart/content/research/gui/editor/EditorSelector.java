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

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.lang.Components;

public class EditorSelector<T> extends BaseEditDialog {
    private final List<Pair<Component, Editor<? extends T>>> editors = new ArrayList<>();
    private final Component label;
    private final T val;
    private final BiPredicate<T, Component> getEditor;
    private final Consumer<T> callback;


    public EditorSelector(UIWidget panel, Component label, BiPredicate<T, Component> pred, T val, Consumer<T> callback) {
        super(panel);
        this.label = label;
        this.val = val;
        this.getEditor = pred;
        this.callback = callback;
    }

    public EditorSelector(UIWidget panel, Component label, Consumer<T> callback) {
        this(panel, label, (o, s) -> false, null, callback);
    }
    public EditorSelector<T> addEditor(String name, Editor<? extends T> e) {
        editors.add(Pair.of(Components.str(name), e));
        return this;
    }
    public EditorSelector<T> addEditor(Component name, Editor<? extends T> e) {
        editors.add(Pair.of(name, e));
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addUIElements() {
        add(EditUtils.getTitle(this, label));
        for (Pair<Component, Editor<? extends T>> ent : editors) {
            add(new OpenEditorButton<T>(this, ent.getFirst(), (Editor<T>) ent.getSecond().and((p, l, v, c) -> close(false)), (val != null && getEditor.test(val, ent.getFirst()) ? val : null), callback));
        }
    }

    @Override
    public void onClose() {
    }

}
