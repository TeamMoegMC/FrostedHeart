/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DetailedCitizenSelectorTest {

	@Test
	void appliesEnterAndExitHysteresis() {
		assertTrue(DetailedCitizenSelector.isEligible(16.0 * 16.0, false));
		assertFalse(DetailedCitizenSelector.isEligible(16.01 * 16.01, false));
		assertTrue(DetailedCitizenSelector.isEligible(20.0 * 20.0, true));
		assertFalse(DetailedCitizenSelector.isEligible(20.01 * 20.01, true));
	}

	@Test
	void selectsNearestWithStableIdTieBreak() {
		DetailedCitizenSelector selector = new DetailedCitizenSelector();
		selector.reset(2);
		selector.addCandidate(3, 4.0, false);
		selector.addCandidate(2, 1.0, false);
		selector.addCandidate(1, 1.0, false);
		selector.select(-1, -1);

		assertEquals(Set.of(1, 2), selected(selector));
	}

	@Test
	void retainedCandidateRanksFourBlocksNearer() {
		DetailedCitizenSelector selector = new DetailedCitizenSelector();
		selector.reset(1);
		selector.addCandidate(1, 14.0 * 14.0, true);
		selector.addCandidate(2, 11.0 * 11.0, false);
		selector.select(-1, -1);
		assertEquals(Set.of(1), selected(selector));

		selector.reset(1);
		selector.addCandidate(1, 14.0 * 14.0, true);
		selector.addCandidate(2, 9.0 * 9.0, false);
		selector.select(-1, -1);
		assertEquals(Set.of(2), selected(selector));
	}

	@Test
	void interactionAndCrosshairTargetsDisplaceNearerNormalCandidates() {
		DetailedCitizenSelector selector = new DetailedCitizenSelector();
		selector.reset(2);
		selector.addCandidate(1, 1.0, false);
		selector.addCandidate(2, 15.0 * 15.0, false);
		selector.addCandidate(3, 16.0 * 16.0, false);
		selector.select(2, 3);

		assertEquals(Set.of(2, 3), selected(selector));
	}

	@Test
	void enforcesZeroAndSixtyFourCapacity() {
		DetailedCitizenSelector selector = new DetailedCitizenSelector();
		selector.reset(0);
		selector.addCandidate(1, 0.0, false);
		selector.select(1, 1);
		assertEquals(0, selector.selectedCount());

		selector.reset(64);
		for (int id = 0; id < 1024; id++)
			selector.addCandidate(id, 4.0 * 4.0, false);
		selector.select(-1, -1);
		Set<Integer> selected = selected(selector);
		assertEquals(64, selected.size());
		for (int id = 0; id < 64; id++)
			assertTrue(selected.contains(id));
	}

	private static Set<Integer> selected(DetailedCitizenSelector selector) {
		Set<Integer> result = new HashSet<>();
		for (int i = 0; i < selector.selectedCount(); i++)
			result.add(selector.selectedIdAt(i));
		return result;
	}
}
