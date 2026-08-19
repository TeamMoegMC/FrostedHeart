/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;

import net.minecraft.world.phys.AABB;

class ClientCitizenCullBoxTest {

	private static final double EPSILON = 1.0e-6;

	@Test
	void standingBoundsCoverBothSnapshots() {
		AABB box = ClientCitizen.createCullingBox(0.0, 2.0, 1.0,
				4.0, 3.0, -2.0, CitizenState.IDLE, 0, false);

		assertBox(box, -0.5, 2.0, -2.5, 4.5, 5.0, 1.5);
	}

	@Test
	void movingBoundsCoverMaximumDeadReckoningExtrapolation() {
		double speed = CitizenState.SPEED[CitizenState.RETURN_HOME] * 20.0 / CitizenState.FIXED_SCALE;
		double extrapolatedX = speed * 1.5;
		AABB box = ClientCitizen.createCullingBox(0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, CitizenState.RETURN_HOME, 0, false);

		assertBox(box, -0.5, 0.0, -0.5, extrapolatedX + 0.5, 2.0, 0.5);
	}

	@Test
	void sleepingBoundsRemainLowAndWide() {
		AABB box = ClientCitizen.createCullingBox(10.0, 20.0, 30.0,
				10.0, 20.0, 30.0, CitizenState.SLEEP, 0, true);

		assertBox(box, 8.65, 20.45, 28.65, 11.35, 20.95, 31.35);
	}

	private static void assertBox(AABB box, double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {
		assertEquals(minX, box.minX, EPSILON);
		assertEquals(minY, box.minY, EPSILON);
		assertEquals(minZ, box.minZ, EPSILON);
		assertEquals(maxX, box.maxX, EPSILON);
		assertEquals(maxY, box.maxY, EPSILON);
		assertEquals(maxZ, box.maxZ, EPSILON);
	}
}
