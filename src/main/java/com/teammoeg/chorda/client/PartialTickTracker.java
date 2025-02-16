package com.teammoeg.chorda.client;
/**
 * Bridge between tick animation and partialticks
 * This would track changes of partialTicks to avoid blink
 * 
 * */
public class PartialTickTracker {
	float lpartialTicks;
	float cpartialTicks;
	public float updateAndGet(float partialTicks) {
		float delta=partialTicks-lpartialTicks;
		if(delta<0){
			delta=1-lpartialTicks+partialTicks;
		}
		cpartialTicks+=delta;
		if(cpartialTicks>1)
			cpartialTicks=1;
		lpartialTicks=partialTicks;
		return cpartialTicks;
	}
	public void reset() {
		cpartialTicks=0;
	}
}
