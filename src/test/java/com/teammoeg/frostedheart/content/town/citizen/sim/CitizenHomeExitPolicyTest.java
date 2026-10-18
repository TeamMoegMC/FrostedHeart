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

package com.teammoeg.frostedheart.content.town.citizen.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class CitizenHomeExitPolicyTest {

	@Test
	void houseLocalSlotsHaveDistinctPreferredExits() {
		BlockPos entrance = new BlockPos(30, 70, -12);
		long sharedHouseDaySeed = 0x123456789ABCDEFL;
		Set<Long> candidates = new HashSet<>();

		for (int slot = 0; slot < 256; slot++) {
			long candidate = CitizenSimScheduler.exitCandidatePosition(
					entrance, slot, sharedHouseDaySeed, 0);
			assertTrue(candidates.add(candidate), "duplicate preferred exit for slot " + slot);
		}
	}

	@Test
	void fallbackProbeOrderIsStableAndDoesNotRepeat() {
		BlockPos entrance = BlockPos.ZERO;
		long seed = -17L;
		Set<Long> candidates = new HashSet<>();

		for (int attempt = 0; attempt < CitizenSimScheduler.EXIT_SEARCH_ATTEMPTS; attempt++) {
			long candidate = CitizenSimScheduler.exitCandidatePosition(entrance, 7, seed, attempt);
			assertEquals(candidate,
					CitizenSimScheduler.exitCandidatePosition(entrance, 7, seed, attempt));
			assertTrue(candidates.add(candidate), "repeated fallback candidate " + attempt);
		}
	}
}
