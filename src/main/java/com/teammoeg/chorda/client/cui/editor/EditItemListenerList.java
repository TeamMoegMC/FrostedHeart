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

import java.util.ArrayList;
import java.util.List;

/**
 * 编辑项变更监听器列表，管理和触发一组变更监听器。
 * <p>
 * Edit item change listener list managing and dispatching a set of change listeners.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public class EditItemListenerList<T> {
	List<EditItemChangeListener<T>> listners=new ArrayList<>();
	/**
	 * 触发所有已注册的监听器。
	 * <p>
	 * Fires all registered listeners.
	 *
	 * @param item   发生变更的编辑项 / the edit item that changed
	 * @param old    旧值 / the old value
	 * @param newval 新值 / the new value
	 */
	public void call(EditItem<T> item,T old,T newval) {
		listners.forEach(t->t.call(item, old, newval));
	}
	/**
	 * 添加一个变更监听器。
	 * <p>
	 * Adds a change listener.
	 *
	 * @param listener 要添加的监听器 / the listener to add
	 */
	public void add(EditItemChangeListener<T> listener) {
		listners.add(listener);
	}
}
