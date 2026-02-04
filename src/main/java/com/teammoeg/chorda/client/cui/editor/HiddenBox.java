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

import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.UIElement;

public class HiddenBox<T> extends UIElement {
	T value;
	public HiddenBox(UIElement parent,T value,Supplier<T> ifEmpty) {
		super(parent);
		if(value==null)
			this.value=ifEmpty.get();
		else
			this.value=value;
	}
	public T getValue() {
		return value;
	}
	public void setValue(T nval) {
		this.value=nval;
	}

}
