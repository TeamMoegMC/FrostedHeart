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
 * Tick动画与partialTicks之间的桥接器，跟踪partialTicks的变化以避免画面闪烁。
 * 提供与tick对齐的平滑过渡值，确保动画在tick边界处不会出现跳变。
 * <p>
 * Bridge between tick-based animation and partialTicks, tracking partialTicks changes
 * to avoid visual blinking. Provides tick-aligned smooth transition values ensuring
 * animations do not jump at tick boundaries.
 */
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
