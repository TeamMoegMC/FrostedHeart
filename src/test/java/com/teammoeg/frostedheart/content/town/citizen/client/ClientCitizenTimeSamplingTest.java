/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenState;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenBatchPacket;
import com.teammoeg.frostedheart.content.town.citizen.sync.S2CCitizenSpawnPacket;
import com.teammoeg.frostedheart.content.town.resident.Resident;

class ClientCitizenTimeSamplingTest {

	@AfterEach
	void clearCache() {
		ClientCitizenCache.clear();
	}

	@Test
	void batchUsesOneTimestampAndReturnsResolvedCitizens() {
		byte stateDir = CitizenState.packStateDir(CitizenState.IDLE, 0);
		ClientCitizenCache.applySpawn(new S2CCitizenSpawnPacket.Entry(
				1, 0, 0, 0, stateDir, (byte) Resident.AGE_ADULT, "One"), 2.0, null);
		ClientCitizenCache.applySpawn(new S2CCitizenSpawnPacket.Entry(
				2, 0, 0, 0, stateDir, (byte) Resident.AGE_ADULT, "Two"), 3.0, null);
		List<ClientCitizen> updated = new ArrayList<>();

		ClientCitizenCache.applyBatch(List.of(new S2CCitizenBatchPacket.Group(0, 0, List.of(
				new S2CCitizenBatchPacket.Entry(1, 16, 0, 0, stateDir),
				new S2CCitizenBatchPacket.Entry(2, 32, 0, 0, stateDir)))), 10.0, updated);

		ClientCitizen first = ClientCitizenCache.get(1);
		ClientCitizen second = ClientCitizenCache.get(2);
		assertEquals(10.0, first.snapshotStartSeconds());
		assertEquals(10.0, second.snapshotStartSeconds());
		assertEquals(0.5, first.renderPos(10.5)[0]);
		assertEquals(1.0, second.renderPos(10.5)[0]);
		assertSame(first, updated.get(0));
		assertSame(second, updated.get(1));
	}
}
