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

package com.teammoeg.chorda.math;

/**
 * 二维受限空间接口，定义在有边界限制的二维空间内移动的行为。
 * <p>
 * Interface for a constrained 2D space, defining movement behavior within a bounded 2D area.
 */
public interface Dimension2D {

	/**
	 * 设置位置坐标，超出边界时进行约束。
	 * <p>
	 * Sets the position coordinates, constraining to boundaries when exceeded.
	 *
	 * @param x X坐标偏移 / the X coordinate offset
	 * @param y Y坐标偏移 / the Y coordinate offset
	 */
	void setPos(float x, float y);

	/**
	 * 将位置重置到原点。
	 * <p>
	 * Resets the position to the origin.
	 */
	void reset();

	/**
	 * 在当前位置上添加偏移量，超出边界时进行约束。
	 * <p>
	 * Adds an offset to the current position, constraining to boundaries when exceeded.
	 *
	 * @param x X方向偏移量 / the X-direction offset
	 * @param y Y方向偏移量 / the Y-direction offset
	 */
	void addPos(float x, float y);

	/**
	 * 获取当前X坐标。
	 * <p>
	 * Gets the current X coordinate.
	 *
	 * @return 当前X坐标 / the current X coordinate
	 */
	float getX();

	/**
	 * 获取当前Y坐标。
	 * <p>
	 * Gets the current Y coordinate.
	 *
	 * @return 当前Y坐标 / the current Y coordinate
	 */
	float getY();

	/**
	 * 以double参数添加位置偏移，内部转换为float。
	 * <p>
	 * Adds position offset with double parameters, internally converting to float.
	 *
	 * @param x X方向偏移量 / the X-direction offset
	 * @param y Y方向偏移量 / the Y-direction offset
	 */
	public default void addPos(double x, double y) {
		this.addPos((float)(x), (float)(y));
	}
}