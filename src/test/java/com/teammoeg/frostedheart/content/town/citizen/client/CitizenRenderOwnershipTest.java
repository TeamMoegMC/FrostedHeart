/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CitizenRenderOwnershipTest {

	@Test
	void detailedOwnershipAppliesOnlyToAwakeCitizens() {
		assertEquals(CitizenRenderOwner.DETAILED_ENTITY,
				CitizenRenderOwnership.resolve(false, true, 16.0 * 16.0));
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderOwnership.resolve(true, true, 16.0 * 16.0));
	}

	@Test
	void bodyAndBillboardBoundariesAreInclusiveAndExclusive() {
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderOwnership.resolve(false, false, 67.99 * 67.99));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderOwnership.resolve(false, false, 68.0 * 68.0));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderOwnership.resolve(false, false, 96.0 * 96.0));
		assertEquals(CitizenRenderOwner.NONE,
				CitizenRenderOwnership.resolve(false, false, 96.01 * 96.01));
	}

	@Test
	void bodyAndBillboardHysteresisUsesThePreviousBatchOwner() {
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderOwnership.resolve(false, false, 70.0 * 70.0,
						CitizenRenderOwner.BODY_BATCH));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderOwnership.resolve(false, false, 70.0 * 70.0,
						CitizenRenderOwner.BILLBOARD_BATCH));
		assertEquals(CitizenRenderOwner.BILLBOARD_BATCH,
				CitizenRenderOwnership.resolve(false, false, 72.01 * 72.01,
						CitizenRenderOwner.BODY_BATCH));
		assertEquals(CitizenRenderOwner.BODY_BATCH,
				CitizenRenderOwnership.resolve(false, false, 67.99 * 67.99,
						CitizenRenderOwner.BILLBOARD_BATCH));
	}

	@Test
	void invalidDistancesHaveNoOwner() {
		assertEquals(CitizenRenderOwner.NONE,
				CitizenRenderOwnership.resolve(false, true, -1.0));
		assertEquals(CitizenRenderOwner.NONE,
				CitizenRenderOwnership.resolve(false, true, Double.NaN));
		assertEquals(CitizenRenderOwner.NONE,
				CitizenRenderOwnership.resolve(false, true, Double.POSITIVE_INFINITY));
	}
}
