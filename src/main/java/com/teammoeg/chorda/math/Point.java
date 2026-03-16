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

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 不可变的二维整数点，支持Codec序列化。
 * <p>
 * An immutable 2D integer point with Codec serialization support.
 */
public class Point {
	public final static Codec<Point> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.INT.fieldOf("x").forGetter(o->o.x),
		Codec.INT.fieldOf("y").forGetter(o->o.y)).apply(t, Point::new));
    protected final int x, y;

    /**
     * 构造一个新的二维点。
     * <p>
     * Constructs a new 2D point.
     *
     * @param x X坐标 / the X coordinate
     * @param y Y坐标 / the Y coordinate
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 获取X坐标。
     * <p>
     * Gets the X coordinate.
     *
     * @return X坐标 / the X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * 获取Y坐标。
     * <p>
     * Gets the Y coordinate.
     *
     * @return Y坐标 / the Y coordinate
     */
    public int getY() {
        return y;
    }
    /**
     * 创建一个新的Point实例的工厂方法。
     * <p>
     * Factory method to create a new Point instance.
     *
     * @param x X坐标 / the X coordinate
     * @param y Y坐标 / the Y coordinate
     * @return 新的Point实例 / a new Point instance
     */
    public static Point of(int x, int y) {
    	return new Point(x, y);
    }

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Point other = (Point) obj;
		return x == other.x && y == other.y;
	}
}
