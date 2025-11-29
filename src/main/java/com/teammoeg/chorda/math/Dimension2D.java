package com.teammoeg.chorda.math;

public interface Dimension2D {

	void setPos(float x, float y);

	void reset();

	void addPos(float x, float y);
	float getX();
	float getY();
	public default void addPos(double x, double y) {
		this.addPos((float)(x), (float)(y));
	}
}