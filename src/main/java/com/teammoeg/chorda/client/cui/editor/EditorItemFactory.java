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

import java.util.function.Function;

import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.editor.EditorDialogBuilder.SetterAndGetter;

public interface EditorItemFactory<T> {
	EditItem<T> create(UILayer l,EditorDialog dialog,T val);

	default <O> SetterAndGetter<O,T> forGetter(Function<O,T> getter){
		return new SetterAndGetter<>(this,getter);
	}
	default SetterAndGetter<T,T> decorator(){
		return new SetterAndGetter<>(this,o->o);
	}
}
