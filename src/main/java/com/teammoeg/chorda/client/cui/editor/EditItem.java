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

import java.util.Optional;

import com.mojang.serialization.DataResult;
import com.teammoeg.chorda.client.cui.base.UIElement;

/**
 * 编辑项接口，封装一个可编辑值及其对应的UI控件。
 * 提供值的获取/设置、变更监听和生命周期回调（创建/保存）。
 * <p>
 * Edit item interface encapsulating an editable value and its corresponding UI widget.
 * Provides value get/set, change listeners, and lifecycle callbacks (create/save).
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public interface EditItem<T> {
	DataResult<Optional<T>> getValue();
	UIElement getWidget();
	void setValue(T val);
	default void onSave() {};
	default void onCreated() {};
	void addOnChangeListener(EditItemChangeListener<T> listener);
}
