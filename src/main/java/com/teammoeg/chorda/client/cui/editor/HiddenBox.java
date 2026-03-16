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

import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.base.UIElement;

/**
 * 隐藏值容器，不显示任何UI但持有一个值。当初始值为null时使用提供的默认值生成器。
 * 用于在编辑器表单中存储不需要用户编辑的自动生成值（如UUID）。
 * <p>
 * Hidden value container that displays no UI but holds a value. Uses the provided
 * default value supplier when the initial value is null. Used in editor forms to
 * store auto-generated values (such as UUIDs) that do not require user editing.
 *
 * @param <T> 存储的值类型 / The type of stored value
 */
public class HiddenBox<T> extends UIElement {
	T value;
	/**
	 * 创建一个隐藏值容器。
	 * <p>
	 * Creates a hidden value container.
	 *
	 * @param parent  父UI元素 / the parent UI element
	 * @param value   初始值 / the initial value
	 * @param ifEmpty 初始值为null时的默认值生成器 / the default value supplier when initial value is null
	 */
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
