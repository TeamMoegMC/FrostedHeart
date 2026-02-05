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

package com.teammoeg.chorda.client;
/**
 * Utility for recording scroll and cast them to int
 * 
 * */
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
