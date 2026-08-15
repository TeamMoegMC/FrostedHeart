/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
