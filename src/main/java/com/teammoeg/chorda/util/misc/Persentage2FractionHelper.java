package com.teammoeg.chorda.util.misc;
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
