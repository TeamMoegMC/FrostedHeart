package com.teammoeg.frostedheart.content.town;

import lombok.Getter;
import net.minecraft.util.Mth;

public class ResourceData {
	private static final double PI=3.0;
	@Getter
	int radius;
	long remain;
	long maximum;
	@Getter
	long extracted;
	public ResourceData(long extracted) {
		this.extracted=extracted;
	}
	public void recalculateResource(double resoucePerSquare) {
		double convertedRadius=Math.sqrt(extracted/PI/resoucePerSquare);//use 3 as pi
		radius=Mth.floor(convertedRadius);
		long oldsize=(long) (PI*resoucePerSquare*radius*radius);
		long newsize=(long) (PI*resoucePerSquare*(radius+1)*(radius+1));
		maximum=newsize-oldsize;
		remain=newsize-extracted;
	}
	public void recoverResource(long number,double resoucePerSquare) {
		extracted-=number;
		if(extracted<0)
			extracted=0;
		remain+=number;
		if(remain>maximum)
			recalculateResource(resoucePerSquare);
	}
	public void costResource(long number,double resoucePerSquare) {
		extracted+=number;
		remain-=number;
		if(remain<=0)
			recalculateResource(resoucePerSquare);
	}
	public long getRemainResource() {
		return remain;
	}
}
