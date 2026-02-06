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
import lombok.Setter;

public class SquareDimension implements Dimension2D {
	@Getter
	@Setter
	int boundsX1;
	@Getter
	@Setter
	int boundsX2;
	@Getter
	@Setter
	int boundsY1;
	@Getter
	@Setter
	int boundsY2;
	@Getter
	float x;
	@Getter
	float y;

	@Override
	public void setPos(float x,float y) {
		if(x<boundsX1)
			x=boundsX1;
		else if(x>boundsX2)
			x=boundsX2;
		if(y<boundsY1)
			y=boundsY1;
		else if(y>boundsY2)
			y=boundsY2;
		this.x=x;
		this.y=y;
	}
	@Override
	public void reset() {
		this.x=0;
		this.y=0;
	}
	@Override
	public void addPos(float x,float y) {
		this.setPos(this.x+x, this.y+y);
	}

	public SquareDimension(int boundsX1, int boundsY1, int boundsX2, int boundsY2) {
		super();
		this.boundsX1 = boundsX1;
		this.boundsX2 = boundsX2;
		this.boundsY1 = boundsY1;
		this.boundsY2 = boundsY2;
	}
}
