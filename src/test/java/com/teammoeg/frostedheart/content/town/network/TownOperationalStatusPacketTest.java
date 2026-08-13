/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TownOperationalStatusPacketTest {
    @AfterEach
    void clearRateLimit() {
        TownOperationalStatusRequestPacket.clearRateLimitsForTests();
    }

    @Test
    void statusPacketRoundTripKeepsMissingMetricsAndAlerts() {
        TownOperationalStatus source = new TownOperationalStatus(
                1234L, 24, 60, 20, 55, 18, 3, 1, 0, 2,
                TownOperationalStatus.Metric.available(6.5),
                TownOperationalStatus.Metric.unavailable(),
                TownOperationalStatus.Metric.available(-2),
                TownOperationalStatus.Metric.unavailable(),
                1, 0,
                new TownOperationalStatus.TowerStatus(TownOperationalStatus.TowerKind.T2,
                        true, true, false, true, 0.75),
                -2,
                List.of(new TownOperationalStatus.ActiveAlert(
                        TownSignalEvent.Type.HOUSE_TEMPERATURE_UNSAFE,
                        TownSignalEvent.Severity.CRITICAL, 1)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        TownOperationalStatusPacketCodec.write(buffer, source);
        assertEquals(source, TownOperationalStatusPacketCodec.read(buffer));
    }

    @Test
    void serverRateLimitAllowsAtMostOneResponsePerSecondAndHandlesClockReset() {
        UUID player = UUID.randomUUID();
        assertTrue(TownOperationalStatusRequestPacket.shouldRespond(player, 100));
        assertFalse(TownOperationalStatusRequestPacket.shouldRespond(player, 119));
        assertTrue(TownOperationalStatusRequestPacket.shouldRespond(player, 120));
        assertTrue(TownOperationalStatusRequestPacket.shouldRespond(player, 5));
    }
}
