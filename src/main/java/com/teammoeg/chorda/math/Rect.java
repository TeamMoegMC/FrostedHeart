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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.client.renderer.Rect2i;

import java.util.Objects;

/**
 * 不可变的矩形类，由左上角坐标和宽高定义，继承自{@link Point}。
 * 提供范围检测、相交计算、扩展等操作。
 * <p>
 * An immutable rectangle defined by top-left coordinates and dimensions, extending {@link Point}.
 * Provides range detection, intersection calculation, expansion and other operations.
 */
@Getter
public class Rect extends Point {
	public final static Codec<Rect> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.INT.fieldOf("x").forGetter(o->o.x),
		Codec.INT.fieldOf("y").forGetter(o->o.y), 
		Codec.INT.fieldOf("w").forGetter(o->o.w),
		Codec.INT.fieldOf("h").forGetter(o->o.h)).apply(t, Rect::new));
	protected final int w, h;
	public static final Rect NONE=new Rect(0,0,0,0);
	/**
	 * 从两个对角点坐标创建矩形。
	 * <p>
	 * Creates a rectangle from two corner point coordinates.
	 *
	 * @param x1 第一个角的X坐标 / X coordinate of the first corner
	 * @param y1 第一个角的Y坐标 / Y coordinate of the first corner
	 * @param x2 第二个角的X坐标 / X coordinate of the second corner
	 * @param y2 第二个角的Y坐标 / Y coordinate of the second corner
	 * @return 新的矩形 / a new rectangle
	 */
	public static Rect delta(int x1, int y1, int x2, int y2) {
		return new Rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	/**
	 * 构造一个新的矩形。
	 * <p>
	 * Constructs a new rectangle.
	 *
	 * @param x 左上角X坐标 / the X coordinate of the top-left corner
	 * @param y 左上角Y坐标 / the Y coordinate of the top-left corner
	 * @param w 宽度 / the width
	 * @param h 高度 / the height
	 */
	public Rect(int x, int y, int w, int h) {
		super(x, y);
		this.w = w;
		this.h = h;
	}

	/**
	 * 复制构造一个矩形。
	 * <p>
	 * Copy constructor for a rectangle.
	 *
	 * @param r 要复制的矩形 / the rectangle to copy
	 */
	public Rect(Rect r) {
		this(r.x, r.y, r.w, r.h);
	}

	/**
	 * 获取矩形右边界的X坐标。
	 * <p>
	 * Gets the X coordinate of the right edge.
	 *
	 * @return 右边界X坐标 / the X coordinate of the right edge
	 */
	public int getX2() {
		return x + w;
	}

	/**
	 * 获取矩形下边界的Y坐标。
	 * <p>
	 * Gets the Y coordinate of the bottom edge.
	 *
	 * @return 下边界Y坐标 / the Y coordinate of the bottom edge
	 */
	public int getY2() {
		return y + h;
	}

	/**
	 * 转换为Minecraft的Rect2i类型。
	 * <p>
	 * Converts to Minecraft's Rect2i type.
	 *
	 * @return 对应的Rect2i实例 / the corresponding Rect2i instance
	 */
	public Rect2i toRect2i() {
		return new Rect2i(x, y, w, h);
	}

	/**
	 * 检查指定坐标是否在矩形范围内。
	 * <p>
	 * Checks whether the specified coordinates are within the rectangle's bounds.
	 *
	 * @param cx 要检查的X坐标 / the X coordinate to check
	 * @param cy 要检查的Y坐标 / the Y coordinate to check
	 * @return 如果点在矩形内则返回true / true if the point is within the rectangle
	 */
	public boolean inRange(int cx, int cy) {
		return (cx >= x && cx <= x + w) && (cy >= y && cy <= y + h);
	}
	/**
	 * 检查指定点是否在矩形范围内。
	 * <p>
	 * Checks whether the specified point is within the rectangle's bounds.
	 *
	 * @param p 要检查的点 / the point to check
	 * @return 如果点在矩形内则返回true / true if the point is within the rectangle
	 */
	public boolean inRange(Point p) {
		return inRange(p.x,p.y);
	}

	/**
	 * 检查此矩形是否与另一个矩形相交。
	 * <p>
	 * Checks whether this rectangle intersects with another rectangle.
	 *
	 * @param other 另一个矩形 / the other rectangle
	 * @return 如果相交则返回true / true if the rectangles intersect
	 */
	public boolean intersects(Rect other) {
		boolean xOverlap = this.x < other.x + other.w &&
			other.x < this.x + this.w;
		boolean yOverlap = this.y < other.y + other.h &&
			other.y < this.y + this.h;
		return xOverlap && yOverlap;
	}
	/**
	 * 计算此矩形与另一个矩形的交集区域。
	 * <p>
	 * Computes the intersection area of this rectangle with another rectangle.
	 *
	 * @param other 另一个矩形 / the other rectangle
	 * @return 交集矩形，如果不相交则返回{@link #NONE} / the intersection rectangle, or {@link #NONE} if they do not intersect
	 */
	public Rect and(Rect other) {
		if(!intersects(other))return Rect.NONE;
		int x1=Math.max(x, other.x);
		int x2=Math.min(getX2(), other.getX2());
		int y1=Math.max(y, other.y);
		int y2=Math.min(getY2(), other.getY2());
		return Rect.delta(x1, y1, x2, y2);
	}
	/**
	 * 按指定的各方向偏移量扩展矩形。
	 * <p>
	 * Expands the rectangle by the specified amounts in each direction.
	 *
	 * @param left 左侧扩展量 / the left expansion amount
	 * @param right 右侧扩展量 / the right expansion amount
	 * @param top 上侧扩展量 / the top expansion amount
	 * @param bottom 下侧扩展量 / the bottom expansion amount
	 * @return 扩展后的新矩形 / a new expanded rectangle
	 */
	public Rect expand(int left,int right,int top,int bottom) {
		return Rect.delta(x-left, y-top, getX2()+right, getY2()+bottom);
	}
	/**
	 * 按X和Y方向的半径对称扩展矩形。
	 * <p>
	 * Expands the rectangle symmetrically by X and Y radii.
	 *
	 * @param xradius X方向扩展半径 / the X-direction expansion radius
	 * @param yradius Y方向扩展半径 / the Y-direction expansion radius
	 * @return 扩展后的新矩形 / a new expanded rectangle
	 */
	public Rect expand(int xradius,int yradius) {
		return expand(xradius,xradius,yradius,yradius);
	}
	/**
	 * 按统一半径对称扩展矩形。
	 * <p>
	 * Expands the rectangle symmetrically by a uniform radius.
	 *
	 * @param radius 扩展半径 / the expansion radius
	 * @return 扩展后的新矩形 / a new expanded rectangle
	 */
	public Rect expand(int radius) {
		return expand(radius,radius);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(h, w);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		Rect other = (Rect) obj;
		return h == other.h && w == other.w;
	}

	@Override
	public String toString() {
		return "Rect [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
	}
	/**
	 * 以角坐标形式返回矩形的字符串表示。
	 * <p>
	 * Returns a string representation of the rectangle using corner coordinates.
	 *
	 * @return 角坐标形式的字符串 / the corner coordinate string representation
	 */
	public String toCornerString() {
		return "Rect [x=" + x + ", y=" + y + ", x2=" + getX2() + ", y2=" + getY2() + "]";
	}
}
