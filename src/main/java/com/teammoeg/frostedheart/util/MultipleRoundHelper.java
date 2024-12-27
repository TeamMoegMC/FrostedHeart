package com.teammoeg.frostedheart.util;

public class MultipleRoundHelper {
	private float reminder=0;
	private int maximum;
	private int originMax;
	public MultipleRoundHelper(int maximum) {
		this.originMax=this.maximum=maximum;
	}
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
	public int getPercentRounded(float value) {
		return getRounded(value*originMax);
	}
}
