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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

class TownHistoryUpdatePacketTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        CUtils.startTestEnvironment();
    }

    @Test
    void dailyHistoryPacketCarriesPersistentTownDay() {
        TownHistoryEntry entry = new TownHistoryEntry(300L, 24, 50, 60, 4);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new TownHistoryUpdatePacket(entry, 87L).encode(buffer);

        TownHistoryUpdatePacket decoded = new TownHistoryUpdatePacket(buffer);

        assertEquals(entry, decoded.entry());
        assertEquals(87L, decoded.townDay());
    }
}
