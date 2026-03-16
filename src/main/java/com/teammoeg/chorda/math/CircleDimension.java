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

/**
 * 圆形约束的二维空间实现，位置被限制在指定半径的圆内。
 * <p>
 * A circular-constrained 2D space implementation where the position is confined within a circle of the specified radius.
 */
public class CircleDimension implements Dimension2D {
	@Getter
	float radius;
	float radiusSquared;
	@Getter
	float x;
	@Getter
	float y;
	private static final float EDGE_THRESOLD=.2f;
	/**
	 * 构造一个指定半径的圆形约束空间。
	 * <p>
	 * Constructs a circular constraint space with the specified radius.
	 *
	 * @param radius 圆的半径 / the radius of the circle
	 */
	public CircleDimension(float radius) {
		super();
		this.setRadius(radius);
	}

	/**
	 * 设置圆的半径并更新半径的平方缓存。
	 * <p>
	 * Sets the radius of the circle and updates the squared radius cache.
	 *
	 * @param radius 新的半径 / the new radius
	 */
	public void setRadius(float radius) {
		this.radius=radius;
		this.radiusSquared=radius*radius;
	}
	
	@Override
	public void setPos(float x, float y) {
		float targetX = this.x + x;
		float targetY = this.y+ y;
	    if ( targetX * targetX + targetY * targetY < radiusSquared-.2f) {
	    	this.x = targetX;
	    	this.y = targetY;
	    } else {
            double theta = Math.atan2(targetY, targetX);
            this.x =  (float) (radius * Math.cos(theta));
            this.y =  (float) (radius * Math.sin(theta));
        } 
	}

	@Override
	public void reset() {
		this.x=0;
		this.y=0;
	}
	
	@Override
	public void addPos(float x, float y) {
		float targetX = this.x + x;
		float targetY = this.y+ y;
		//if the target position is inside the circle
	    if ( targetX * targetX + targetY * targetY < radiusSquared) {
	    	this.x = targetX;
	    	this.y = targetY;
	    } else {
	    	//if previous point is inside the circle
	        //if ( radiusSquared-EDGE_THRESOLD > this.x * this.x + this.y * this.y) {
	        	
	            double theta = Math.atan2(targetY, targetX);
	            this.x =  (float) (radius * Math.cos(theta));
	            this.y =  (float) (radius * Math.sin(theta));
	       /* } else { // previous point at edge
	           
	        	double thetaPrev = Math.atan2(this.x, this.y);
	            // Calculate movement in tangent direction
	        	double tangentDisp = x * (-Math.sin(thetaPrev)) + y * Math.cos(thetaPrev);
	        	double thetaCurrent = thetaPrev + tangentDisp / radius;
	        	this.x = (float) (radius * Math.cos(thetaCurrent));
	        	this.y = (float) (radius * Math.sin(thetaCurrent));
	        }*/
	    }
	}
	
	public static void main(String[] args) {
		CircleDimension cdm=new CircleDimension(125);
		cdm.addPos(-250, 250);
		cdm.addPos(0, 250);
		System.out.println(cdm);
	}

	@Override
	public String toString() {
		return "CircleDimension [radius=" + radius + ", x=" + x + ", y=" + y + "]";
	}

}
