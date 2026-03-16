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

package com.teammoeg.chorda.client;
/**
 * 滚动量累积追踪器，将浮点数滚动值累积并转换为整数滚动步数。
 * 用于处理高精度触控板等设备产生的小数滚动值，当方向改变时自动重置累积量。
 * <p>
 * Scroll accumulation tracker that accumulates floating-point scroll values and
 * converts them to integer scroll steps. Used for handling fractional scroll values
 * from high-precision trackpads; automatically resets accumulation when direction changes.
 */
public class ScrollTracker {
	double accumulatedValue;
	public void clear() {
		accumulatedValue=0;
	}
	public void addScroll(double value) {
		if(Math.signum(accumulatedValue)!=Math.signum(value)) {
			accumulatedValue=0;
		}
		accumulatedValue+=value;
		
	}
	public int getScroll() {
		int ret=(int)accumulatedValue;
		accumulatedValue-=ret;
		return ret;
	}
}
