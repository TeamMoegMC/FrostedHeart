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

package com.teammoeg.chorda.client.cui.base;

/**
 * 可获得焦点的UI元素接口。实现此接口的UI元素可以接收键盘输入焦点。
 * 焦点由PrimaryLayer统一管理，同一时间只有一个元素可以获得焦点。
 * <p>
 * Interface for UI elements that can receive input focus. Elements implementing this
 * interface can receive keyboard input focus. Focus is managed centrally by PrimaryLayer,
 * with only one element focused at a time.
 */
public interface Focusable{
    boolean isFocused();

    void setFocused(boolean v);


}
