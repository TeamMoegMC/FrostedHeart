package com.teammoeg.frostedheart.content.town;

import lombok.Getter;
import net.minecraft.util.Mth;

public class ResourceData {
	private static final double PI=3.0;
	@Getter
	int radius;
	@Getter
	long extracted;
	long total;
	public ResourceData() {
	}
	public ResourceData(long extracted) {
		this.extracted=extracted;
	}
	public void recalculateRadius(double resoucePerSquare,int maxradius) {
		if(resoucePerSquare<=0)return;
		double convertedRadius=Math.sqrt(extracted/PI/resoucePerSquare);//use 3 as pi
		total=(long) (PI*resoucePerSquare*maxradius*maxradius);
		
		radius=Mth.floor(convertedRadius)+1;
	}
	public void recoverResource(long number) {
		extracted-=number;
		if(extracted<0)
			extracted=0;
	}
	public void costResource(long number) {
		extracted+=number;
	}
	public long getRemainResource() {
		return total-extracted;
	}
	public double getSize() {
		return PI*radius*radius;
	}

}
