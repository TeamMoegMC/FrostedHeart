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
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatus;
import com.teammoeg.frostedheart.content.town.observation.TownSignalEvent;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

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
