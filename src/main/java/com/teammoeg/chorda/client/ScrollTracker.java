package com.teammoeg.chorda.client;

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
