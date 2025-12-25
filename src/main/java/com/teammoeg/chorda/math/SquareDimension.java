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
