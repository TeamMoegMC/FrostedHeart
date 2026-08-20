/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.citizen.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Splits chunk-grouped citizen deltas into packets without dropping entries. */
final class CitizenDeltaPacketBatcher {

	private CitizenDeltaPacketBatcher() {
	}

	static void forEachPacket(List<S2CCitizenBatchPacket.Group> groups, int maxEntriesPerPacket,
			Consumer<List<S2CCitizenBatchPacket.Group>> packetConsumer) {
		if (maxEntriesPerPacket <= 0)
			throw new IllegalArgumentException("maxEntriesPerPacket must be positive");

		List<S2CCitizenBatchPacket.Group> packet = new ArrayList<>();
		int packetSize = 0;
		for (S2CCitizenBatchPacket.Group group : groups) {
			List<S2CCitizenBatchPacket.Entry> entries = group.entries();
			for (int offset = 0; offset < entries.size();) {
				if (packetSize == maxEntriesPerPacket) {
					packetConsumer.accept(packet);
					packet = new ArrayList<>();
					packetSize = 0;
				}

				int take = Math.min(maxEntriesPerPacket - packetSize, entries.size() - offset);
				if (offset == 0 && take == entries.size()) {
					packet.add(group);
				} else {
					packet.add(new S2CCitizenBatchPacket.Group(group.cx(), group.cz(),
							new ArrayList<>(entries.subList(offset, offset + take))));
				}
				offset += take;
				packetSize += take;
			}
		}
		if (packetSize > 0)
			packetConsumer.accept(packet);
	}
}
