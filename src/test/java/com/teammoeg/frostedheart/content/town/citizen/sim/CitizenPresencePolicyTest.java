/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;

class CitizenPresencePolicyTest {

	@Test
	void stateMatrixSeparatesSchedulingFromWorldPresenceAndInteraction() {
		int[][] matrix = {
				{ CitizenState.IDLE, 1, 1, 1, 1 },
				{ CitizenState.WANDER, 1, 1, 1, 1 },
				{ CitizenState.RETURN_HOME, 1, 1, 1, 1 },
				{ CitizenState.SLEEP, 1, 0, 0, 0 },
				{ CitizenState.WORK, 1, 1, 1, 1 }
		};

		for (int[] row : matrix) {
			int state = row[0];
			assertEquals(row[1] != 0, CitizenPresence.behaviorScheduled(state), "behavior state " + state);
			assertEquals(row[2] != 0, CitizenPresence.movementIntegrated(state), "movement state " + state);
			assertEquals(row[3] != 0, CitizenPresence.spatialPresent(state), "spatial state " + state);
			assertEquals(row[4] != 0, CitizenPresence.interactionAllowed(state), "interaction state " + state);
		}
	}

	@Test
	void invalidStatesHaveNoRuntimePresence() {
		for (int state : new int[] { -1, CitizenState.STATE_COUNT, 255 }) {
			assertFalse(CitizenPresence.behaviorScheduled(state));
			assertFalse(CitizenPresence.movementIntegrated(state));
			assertFalse(CitizenPresence.spatialPresent(state));
			assertFalse(CitizenPresence.interactionAllowed(state));
		}
	}

	@Test
	void onlyVerifiedBedSleepersArePresentationEligible() {
		CitizenSim sim = new CitizenSim(2);
		int awake = sim.add(11, 0, 0, 0);
		int sleeper = sim.add(12, 0, 0, 0);
		sim.state[awake] = CitizenState.IDLE;
		sim.state[sleeper] = CitizenState.SLEEP;

		assertTrue(CitizenPresence.presentationEligible(sim, awake));
		assertFalse(CitizenPresence.presentationEligible(sim, sleeper));
		sim.presentationFlags[sleeper] = CitizenSim.PRESENT_ON_VALID_BED;
		assertTrue(CitizenPresence.presentationEligible(sim, sleeper));
		assertFalse(CitizenPresence.interactionAllowed(sim.state[sleeper]));
	}

	@Test
	void spatialGridExcludesSleepingCitizens() {
		TownSimData container = new TownSimData();
		CitizenSim sim = container.sim();
		int awake = sim.add(101, 512, 64 * 1024, 512, (byte) 0);
		int sleeping = sim.add(102, 768, 64 * 1024, 768, (byte) 0);
		sim.state[awake] = CitizenState.IDLE;
		sim.state[sleeping] = CitizenState.SLEEP;

		SpatialGrid grid = new SpatialGrid();
		grid.rebuild(List.of(container),
				(c, i) -> CitizenPresence.spatialPresent(c.sim().state[i]));
		IntArrayList neighbors = new IntArrayList();
		grid.queryNeighbors(0, 0, neighbors);

		assertTrue(neighbors.contains(101));
		assertFalse(neighbors.contains(102));
		assertEquals(1, neighbors.size());
	}
}
