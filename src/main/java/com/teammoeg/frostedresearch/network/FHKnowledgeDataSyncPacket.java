/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.api.ClientKnowledgeDataAPI;
import com.teammoeg.frostedresearch.compat.ResearchJeiBridge;
import com.teammoeg.frostedresearch.knowledge.KnowledgeSyncSnapshot;
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
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> installClient(snapshot));
            }
        });
        context.get().setPacketHandled(true);
    }

    private static void installClient(KnowledgeSyncSnapshot snapshot) {
        try {
            CClientTeamDataManager.INSTANCE.getInstance().setData(
                    FRSpecialDataTypes.KNOWLEDGE_DATA, snapshot.teamData());
            ClientKnowledgeDataAPI.install(snapshot.catalogRevision(), snapshot.knowledge(), snapshot.technology());
            ResearchJeiBridge.sync();
        } catch (RuntimeException exception) {
            FRMain.LOGGER.error("Failed to install knowledge snapshot", exception);
        }
    }
}
