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
