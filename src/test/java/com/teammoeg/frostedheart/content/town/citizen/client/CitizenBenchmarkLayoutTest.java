/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CitizenBenchmarkLayoutTest {

	@Test
	void acceptsOnlyDocumentedBenchmarkCounts() {
		assertTrue(CitizenBenchmarkLayout.isSupportedCount(32));
		assertTrue(CitizenBenchmarkLayout.isSupportedCount(64));
		assertTrue(CitizenBenchmarkLayout.isSupportedCount(256));
		assertTrue(CitizenBenchmarkLayout.isSupportedCount(1024));
		assertFalse(CitizenBenchmarkLayout.isSupportedCount(0));
		assertFalse(CitizenBenchmarkLayout.isSupportedCount(128));
	}

	@Test
	void laysOutAThousandCitizensInAStableForwardGrid() {
		assertEquals(32, CitizenBenchmarkLayout.sideFor(1024));
		assertEquals(-11_904, CitizenBenchmarkLayout.lateralOffsetFixed(0, 1024));
		assertEquals(11_904, CitizenBenchmarkLayout.lateralOffsetFixed(31, 1024));
		assertEquals(6_144, CitizenBenchmarkLayout.forwardOffsetFixed(0, 1024));
		assertEquals(29_952, CitizenBenchmarkLayout.forwardOffsetFixed(1023, 1024));
	}

	@Test
	void movementIsBoundedPeriodicAndReversesAtEndpoints() {
		assertEquals(-2_048, CitizenBenchmarkLayout.movementOffsetFixed(0, 0));
		assertEquals(1_984, CitizenBenchmarkLayout.movementOffsetFixed(63, 0));
		assertEquals(2_048, CitizenBenchmarkLayout.movementOffsetFixed(64, 0));
		assertEquals(-1_984, CitizenBenchmarkLayout.movementOffsetFixed(127, 0));
		assertEquals(-2_048, CitizenBenchmarkLayout.movementOffsetFixed(128, 0));
		assertTrue(CitizenBenchmarkLayout.movingPositive(63, 0));
		assertFalse(CitizenBenchmarkLayout.movingPositive(64, 0));
	}
}
