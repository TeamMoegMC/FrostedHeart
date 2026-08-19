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

/** Pure ownership policy shared by the coordinator, CPU backend, and tests. */
final class CitizenRenderOwnership {

	static final double BODY_ENTER_DIST2 = 68.0 * 68.0;
	static final double BODY_EXIT_DIST2 = 72.0 * 72.0;
	static final double MAX_DIST2 = 96.0 * 96.0;

	private CitizenRenderOwnership() {
	}

	static CitizenRenderOwner resolve(boolean sleeping, boolean detailedActive, double distance2) {
		return resolve(sleeping, detailedActive, distance2, CitizenRenderOwner.NONE);
	}

	static CitizenRenderOwner resolve(boolean sleeping, boolean detailedActive, double distance2,
			CitizenRenderOwner previousBatchOwner) {
		if (!Double.isFinite(distance2) || distance2 < 0.0 || distance2 > MAX_DIST2)
			return CitizenRenderOwner.NONE;
		if (!sleeping && detailedActive)
			return CitizenRenderOwner.DETAILED_ENTITY;
		if (previousBatchOwner == CitizenRenderOwner.BODY_BATCH)
			return distance2 <= BODY_EXIT_DIST2
					? CitizenRenderOwner.BODY_BATCH : CitizenRenderOwner.BILLBOARD_BATCH;
		if (previousBatchOwner == CitizenRenderOwner.BILLBOARD_BATCH)
			return distance2 < BODY_ENTER_DIST2
					? CitizenRenderOwner.BODY_BATCH : CitizenRenderOwner.BILLBOARD_BATCH;
		if (distance2 < BODY_ENTER_DIST2)
			return CitizenRenderOwner.BODY_BATCH;
		return CitizenRenderOwner.BILLBOARD_BATCH;
	}
}
