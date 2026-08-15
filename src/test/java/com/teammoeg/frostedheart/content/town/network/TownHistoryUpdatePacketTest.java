/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownHistoryUpdatePacketTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
