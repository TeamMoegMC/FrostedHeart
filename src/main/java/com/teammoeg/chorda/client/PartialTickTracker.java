package com.teammoeg.chorda.client;

import net.minecraft.client.Minecraft;

/**
 * Bridge between tick animation and partialticks
 * This would track changes of partialTicks to avoid blink
 * 
 * */
public class PartialTickTracker {
	private static final PartialTickTracker INSTANCE=new PartialTickTracker();
	long ltimeMs;
	float cpartialTicks;
	public float advanceTimer() {
		Minecraft mc=Minecraft.getInstance();
		float delta=mc.getDeltaFrameTime();
		cpartialTicks+=delta;
		if(cpartialTicks>1)
			cpartialTicks=1;
		return cpartialTicks;
	}
	public void tick() {
		cpartialTicks=0;
	}
	public static PartialTickTracker getInstance() {
		return INSTANCE;
	}
	public static float getTickAlignedPartialTicks() {
		return INSTANCE.cpartialTicks;
	}
}
