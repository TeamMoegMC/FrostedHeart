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

import lombok.Getter;
import net.minecraft.client.renderer.Rect2i;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

@Getter
public class Rect extends Point {
	public final static Codec<Rect> CODEC=RecordCodecBuilder.create(t->t.group(
		Codec.INT.fieldOf("x").forGetter(o->o.x),
		Codec.INT.fieldOf("y").forGetter(o->o.y), 
		Codec.INT.fieldOf("w").forGetter(o->o.w),
		Codec.INT.fieldOf("h").forGetter(o->o.h)).apply(t, Rect::new));
	protected final int w, h;
	public static final Rect NONE=new Rect(0,0,0,0);
	public static Rect delta(int x1, int y1, int x2, int y2) {
		return new Rect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	public Rect(int x, int y, int w, int h) {
		super(x, y);
		this.w = w;
		this.h = h;
	}

	public Rect(Rect r) {
		this(r.x, r.y, r.w, r.h);
	}

	public int getX2() {
		return x + w;
	}

	public int getY2() {
		return y + h;
	}

	public Rect2i toRect2i() {
		return new Rect2i(x, y, w, h);
	}

	public boolean inRange(int cx, int cy) {
		return (cx >= x && cx <= x + w) && (cy >= y && cy <= y + h);
	}
	public boolean inRange(Point p) {
		return inRange(p.x,p.y);
	}

	public boolean intersects(Rect other) {
		boolean xOverlap = this.x < other.x + other.w &&
			other.x < this.x + this.w;
		boolean yOverlap = this.y < other.y + other.h &&
			other.y < this.y + this.h;
		return xOverlap && yOverlap;
	}
	public Rect and(Rect other) {
		if(!intersects(other))return Rect.NONE;
		int x1=Math.max(x, other.x);
		int x2=Math.min(getX2(), other.getX2());
		int y1=Math.max(y, other.y);
		int y2=Math.min(getY2(), other.getY2());
		return Rect.delta(x1, y1, x2, y2);
	}
	public Rect expand(int left,int right,int top,int bottom) {
		return Rect.delta(x-left, y-top, getX2()+right, getY2()+bottom);
	}
	public Rect expand(int xradius,int yradius) {
		return expand(xradius,xradius,yradius,yradius);
	}
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
	public String toCornerString() {
		return "Rect [x=" + x + ", y=" + y + ", x2=" + getX2() + ", y2=" + getY2() + "]";
	}
}
