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

/**
 * 可编辑UI元素接口，支持设置值变更监听器。
 * <p>
 * Editable UI element interface supporting change listener registration.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public interface EditableUIElement<T> {
	public void setOnChangeListener(EditItemChangeListener<T> listener);
}
