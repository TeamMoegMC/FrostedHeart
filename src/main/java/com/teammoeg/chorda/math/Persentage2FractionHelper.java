/*
 * Copyright (c) 2024 TeamMoeg
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
 * A utility for rounding percentage into fractions of custom value preserves total parts.
 * 
 * */
public class Persentage2FractionHelper {
	private float reminder=0;
	private int maximum;
	private int originMax;
	public Persentage2FractionHelper(int denum) {
		this.originMax=this.maximum=denum;
	}
	/**
	 * round fraction numer with decimal to integer
	 * 
	 * */
	public int getRounded(float value) {
		value-=reminder;
		int retval=Math.round(value);
		reminder=retval-value;
		if(retval>maximum)
			retval=maximum;
		maximum-=retval;
		return retval;
	}
	public int getReminder() {
		return maximum;
	}
	/*
	* round percentage to fraction numer
	* @Param value percentage value, 100% is 1 and etc
	*/
	public int getPercentRounded(float value) {
		return getRounded(value*originMax);
	}
}
