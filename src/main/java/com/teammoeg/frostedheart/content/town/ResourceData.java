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

package com.teammoeg.frostedheart.content.town;

import lombok.Getter;
import net.minecraft.util.Mth;

public class ResourceData {
	private static final double PI=3.0;
	@Getter
	int radius;
	@Getter
	double extracted;
	double total;
	public ResourceData() {
	}
	public ResourceData(double extracted) {
		this.extracted=extracted;
	}
	public void recalculateRadius(double resoucePerSquare,int maxradius) {
		if(resoucePerSquare<=0)return;
		double convertedRadius=Math.sqrt(extracted/PI/resoucePerSquare);//use 3 as pi
		total=(PI*resoucePerSquare*maxradius*maxradius);
		
		radius=Mth.floor(convertedRadius)+1;
	}
	public void recoverResource(double number) {
		extracted-=number;
		if(extracted<0)
			extracted=0;
	}
	public void costResource(double number) {
		extracted+=number;
	}
	public double getRemainResource() {
		return total-extracted;
	}
	public double getSize() {
		return PI*radius*radius;
	}
	public double mayCostResource(double d) {
		return Math.min(total-extracted, d);
		
	}

}
