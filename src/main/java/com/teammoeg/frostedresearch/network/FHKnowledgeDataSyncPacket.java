/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.network;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.knowledge.KnowledgeSyncSnapshot;
import com.teammoeg.frostedresearch.network.client.ClientKnowledgeSnapshotHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Full replacement packet for V2 knowledge and its compiled read models. */
public final class FHKnowledgeDataSyncPacket implements CMessage {
    private CompoundTag payload;

    public FHKnowledgeDataSyncPacket(FriendlyByteBuf buffer) {
        payload = ResearchNetworkCodec.readPayload(buffer, "knowledge snapshot");
    }

    public FHKnowledgeDataSyncPacket(TeamDataHolder team) {
        payload = ResearchNetworkCodec.encode(KnowledgeSyncSnapshot.CODEC, KnowledgeSyncSnapshot.create(team));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(payload);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            KnowledgeSyncSnapshot snapshot = ResearchNetworkCodec.decode(
                    KnowledgeSyncSnapshot.CODEC, payload, "knowledge snapshot");
            if (snapshot != null) {
                // A packet snapshot is runtime data, so the runnable must capture it. Forge's
                // safe referent API only accepts a parameterless method reference and rejects
                // such a capture in development. Keep the physical-side boundary here and all
                // client installation code in the dedicated client-only handler.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientKnowledgeSnapshotHandler.install(snapshot));
            }
        });
        context.get().setPacketHandled(true);
    }
}
