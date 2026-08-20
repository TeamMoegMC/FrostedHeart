/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

/** Pure deterministic layout math shared by the in-game citizen benchmark and tests. */
final class CitizenBenchmarkLayout {

	static final int[] SUPPORTED_COUNTS = { 32, 64, 256, 1024 };
	static final int SPACING_FIXED = 768;
	static final int NEAR_DISTANCE_FIXED = 6 * 1024;
	private static final int MOVEMENT_AMPLITUDE_FIXED = 2 * 1024;
	private static final int MOVEMENT_PERIOD = 128;

	private CitizenBenchmarkLayout() {
	}

	static boolean isSupportedCount(int count) {
		for (int supported : SUPPORTED_COUNTS) {
			if (count == supported)
				return true;
		}
		return false;
	}

	static int sideFor(int count) {
		int side = 1;
		while (side * side < count)
			side++;
		return side;
	}

	static int lateralOffsetFixed(int index, int count) {
		int side = sideFor(count);
		int column = index % side;
		return (2 * column - side + 1) * SPACING_FIXED / 2;
	}

	static int forwardOffsetFixed(int index, int count) {
		return NEAR_DISTANCE_FIXED + index / sideFor(count) * SPACING_FIXED;
	}

	static int movementOffsetFixed(long tick, int index) {
		int phase = (int) Math.floorMod(tick + index * 11L, MOVEMENT_PERIOD);
		if (phase < MOVEMENT_PERIOD / 2)
			return -MOVEMENT_AMPLITUDE_FIXED + phase * 64;
		return MOVEMENT_AMPLITUDE_FIXED - (phase - MOVEMENT_PERIOD / 2) * 64;
	}

	static boolean movingPositive(long tick, int index) {
		return Math.floorMod(tick + index * 11L, MOVEMENT_PERIOD) < MOVEMENT_PERIOD / 2;
	}
}
