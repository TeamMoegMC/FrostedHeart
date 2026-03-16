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
 * 比例滚动条接口，提供基于0.0到1.0比例值的滚动位置访问。
 * 当UILayer中存在实现此接口的元素时，该层的默认滚动行为会被禁用。
 * <p>
 * Ratio-based scrollbar interface providing scroll position access via a 0.0 to 1.0
 * ratio value. When an element implementing this interface exists in a UILayer,
 * the layer's default scroll behavior is disabled.
 */
public interface RatedScrollbar {
	public float getScrollRatio();
	public void setScrollRatio(float value);
}
