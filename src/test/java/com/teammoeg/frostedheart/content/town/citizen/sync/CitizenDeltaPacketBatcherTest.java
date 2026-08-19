/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.citizen.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CitizenDeltaPacketBatcherTest {

	@Test
	void rejectsNonPositivePacketLimit() {
		assertThrows(IllegalArgumentException.class,
				() -> CitizenDeltaPacketBatcher.forEachPacket(List.of(), 0, packet -> {
				}));
	}

	@Test
	void preservesExactEntryBoundaries() {
		for (int count : List.of(0, 1, 239, 240, 241, 480, 481, 1024)) {
			List<List<S2CCitizenBatchPacket.Group>> packets = collectPackets(
					List.of(group(7, 11, count, 1000)), 240);
			assertEquals(expectedPacketCount(count), packets.size());
			assertEquals(packetSizes(count), packets.stream().mapToInt(this::size).boxed().toList());
			assertExactIds(packets, count, 1000);
		}
	}

	@Test
	void splitsAChunkGroupWithoutChangingItsCoordinates() {
		List<List<S2CCitizenBatchPacket.Group>> packets = collectPackets(
				List.of(group(-3, 19, 481, 2000)), 240);

		assertEquals(List.of(240, 240, 1), packets.stream().mapToInt(this::size).boxed().toList());
		for (List<S2CCitizenBatchPacket.Group> packet : packets)
			for (S2CCitizenBatchPacket.Group group : packet) {
				assertEquals(-3, group.cx());
				assertEquals(19, group.cz());
			}
		assertExactIds(packets, 481, 2000);
	}

	@Test
	void preservesMultipleChunkGroupsAcrossPacketBoundaries() {
		List<List<S2CCitizenBatchPacket.Group>> packets = collectPackets(List.of(
				group(0, 0, 150, 1), group(1, 0, 150, 1001), group(2, 0, 10, 2001)), 240);

		assertEquals(List.of(240, 70), packets.stream().mapToInt(this::size).boxed().toList());
		Set<Integer> ids = ids(packets);
		assertEquals(310, ids.size());
		assertContainsRange(ids, 1, 150);
		assertContainsRange(ids, 1001, 150);
		assertContainsRange(ids, 2001, 10);
		Set<Long> coordinates = new HashSet<>();
		for (List<S2CCitizenBatchPacket.Group> packet : packets)
			for (S2CCitizenBatchPacket.Group group : packet)
				coordinates.add((((long) group.cx()) << 32) ^ (group.cz() & 0xFFFFFFFFL));
		assertEquals(Set.of(0L, 1L << 32, 2L << 32), coordinates);
	}

	@Test
	void reusesWholeGroupsAndCopiesOnlySplitFragments() {
		S2CCitizenBatchPacket.Group whole = group(4, 5, 100, 1);
		List<List<S2CCitizenBatchPacket.Group>> wholePackets = collectPackets(List.of(whole), 240);
		assertSame(whole, wholePackets.get(0).get(0));

		S2CCitizenBatchPacket.Group split = group(6, 7, 241, 1000);
		List<List<S2CCitizenBatchPacket.Group>> splitPackets = collectPackets(List.of(split), 240);
		assertNotSame(split, splitPackets.get(0).get(0));
		assertNotSame(split, splitPackets.get(1).get(0));
	}

	private List<List<S2CCitizenBatchPacket.Group>> collectPackets(
			List<S2CCitizenBatchPacket.Group> groups, int limit) {
		List<List<S2CCitizenBatchPacket.Group>> packets = new ArrayList<>();
		CitizenDeltaPacketBatcher.forEachPacket(groups, limit, packets::add);
		return packets;
	}

	private S2CCitizenBatchPacket.Group group(int cx, int cz, int count, int firstId) {
		List<S2CCitizenBatchPacket.Entry> entries = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			entries.add(new S2CCitizenBatchPacket.Entry(firstId + i, i & 0xFF, 0, i & 0xFF, (byte) 0));
		return new S2CCitizenBatchPacket.Group(cx, cz, entries);
	}

	private int size(List<S2CCitizenBatchPacket.Group> packet) {
		return packet.stream().mapToInt(group -> group.entries().size()).sum();
	}

	private List<Integer> packetSizes(int count) {
		List<Integer> result = new ArrayList<>();
		for (int remaining = count; remaining > 0; remaining -= 240)
			result.add(Math.min(remaining, 240));
		return result;
	}

	private int expectedPacketCount(int count) {
		return (count + 239) / 240;
	}

	private void assertExactIds(List<List<S2CCitizenBatchPacket.Group>> packets, int count, int firstId) {
		Set<Integer> ids = ids(packets);
		assertEquals(count, ids.size());
		assertContainsRange(ids, firstId, count);
	}

	private Set<Integer> ids(List<List<S2CCitizenBatchPacket.Group>> packets) {
		Set<Integer> ids = new HashSet<>();
		for (List<S2CCitizenBatchPacket.Group> packet : packets)
			for (S2CCitizenBatchPacket.Group group : packet)
				for (S2CCitizenBatchPacket.Entry entry : group.entries())
					ids.add(entry.id());
		return ids;
	}

	private void assertContainsRange(Set<Integer> ids, int firstId, int count) {
		for (int i = 0; i < count; i++)
			assertTrue(ids.contains(firstId + i));
	}
}
