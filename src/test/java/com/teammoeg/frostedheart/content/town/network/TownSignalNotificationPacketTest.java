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

package com.teammoeg.frostedheart.content.town.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

class TownSignalNotificationPacketTest {
    @Test
    void packetRoundTripContainsOnlySafeNoticeFields() {
        TownSignalNotificationPacket source = new TownSignalNotificationPacket(42L, List.of(
                new TownSignalNotice(TownSignalEvent.Type.RESIDENT_EXIT_HEALTH,
                        TownSignalEvent.Severity.IRREVERSIBLE, 3),
                new TownSignalNotice(TownSignalEvent.Type.FUEL_SHORTAGE,
                        TownSignalEvent.Severity.CRITICAL, 1)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            source.encode(buffer);
            TownSignalNotificationPacket decoded = new TownSignalNotificationPacket(buffer);
            assertEquals(source, decoded);
        } finally {
            buffer.release();
        }
    }
}
