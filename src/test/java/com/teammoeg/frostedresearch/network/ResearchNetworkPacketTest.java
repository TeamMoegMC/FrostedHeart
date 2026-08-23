/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.network;

import com.teammoeg.frostedresearch.data.ClueData;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.research.Research;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchNetworkPacketTest {
    @Test
    void keyedResearchUpdateRoundTripsLongsAndCustomClueNbt() {
        Research research = new Research();
        research.setId("stable-research");
        CompoundTag custom = new CompoundTag();
        custom.putString("payload", "kept");
        ClueData clue = new ClueData(true);
        clue.setData(custom);
        ResearchData source = new ResearchData(
                5_000_000_000L, new boolean[]{true, false}, 2,
                Map.of("stable-clue", clue), Map.of("stable-effect", true));
        FHResearchDataUpdatePacket packet = new FHResearchDataUpdatePacket(research, source);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        packet.encode(buffer);
        FHResearchDataUpdatePacket decodedPacket = new FHResearchDataUpdatePacket(buffer);
        ResearchData decoded = ResearchNetworkCodec.decode(
                ResearchData.NETWORK_CODEC, decodedPacket.rd(), "test");

        assertEquals("stable-research", decodedPacket.researchId());
        assertEquals(5_000_000_000L, decoded.getCommitted());
        assertEquals("kept", decoded.getClueData().get("stable-clue").getData().getString("payload"));
        assertTrue(decoded.isEffectGranted("stable-effect"));
    }

    @Test
    void malformedAndOversizedPacketFieldsProduceInertPackets() {
        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeNbt(null);
        oversized.writeUtf("x".repeat(129));
        FHResearchDataUpdatePacket update = assertDoesNotThrow(
                () -> new FHResearchDataUpdatePacket(oversized));
        assertNull(update.rd());
        assertNull(update.researchId());

        FriendlyByteBuf invalidControl = new FriendlyByteBuf(Unpooled.buffer());
        invalidControl.writeUtf("known-shape");
        invalidControl.writeByte(255);
        assertNull(new FHResearchControlPacket(invalidControl).status);

        assertDoesNotThrow(() -> new FHS2CClueProgressSyncPacket(
                new FriendlyByteBuf(Unpooled.buffer())));
        assertDoesNotThrow(() -> new FHDrawingDeskOperationPacket(
                new FriendlyByteBuf(Unpooled.buffer())));
        assertDoesNotThrow(() -> new FHResearchRegistrtySyncPacket(
                new FriendlyByteBuf(Unpooled.buffer())));
        assertDoesNotThrow(() -> new FHResearchAttributeSyncPacket(
                new FriendlyByteBuf(Unpooled.buffer())));
        FHInsightSyncPacket insight = assertDoesNotThrow(() -> new FHInsightSyncPacket(
                new FriendlyByteBuf(Unpooled.buffer())));
        assertEquals(-1, insight.insight());
    }
}
