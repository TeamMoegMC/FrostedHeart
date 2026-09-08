/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.network;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.KnowledgeSyncSnapshot;
import com.teammoeg.frostedresearch.network.FHKnowledgeDataSyncPacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * One fragment of a single atomic knowledge state/projection/view replacement.
 */
public final class KnowledgeSnapshotPacket implements CMessage {
    private static final KnowledgeSnapshotTransfer.Accumulator CLIENT = new KnowledgeSnapshotTransfer.Accumulator();
    private final KnowledgeSnapshotTransfer.Fragment fragment;

    public KnowledgeSnapshotPacket(KnowledgeSnapshotTransfer.Fragment fragment) {
        this.fragment = fragment;
    }

    public KnowledgeSnapshotPacket(FriendlyByteBuf buffer) {
        fragment = new KnowledgeSnapshotTransfer.Fragment(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readByteArray(KnowledgeSnapshotTransfer.CHUNK_BYTES));
    }

    public static List<KnowledgeSnapshotPacket> create(TeamDataHolder team) {
        CompoundTag tag = (CompoundTag) KnowledgeSyncSnapshot.CODEC.encodeStart(NbtOps.INSTANCE,
                KnowledgeSyncSnapshot.create(team)).getOrThrow(false, message -> FRMain.LOGGER.error("Knowledge snapshot: {}", message));
        try {
            return KnowledgeSnapshotTransfer.split(tag).stream().map(KnowledgeSnapshotPacket::new).toList();
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException("Could not encode knowledge snapshot", exception);
        }
    }

    public static void send(ServerPlayer player, TeamDataHolder team) {
        for (KnowledgeSnapshotPacket packet : create(team)) FRNetwork.INSTANCE.sendPlayer(player, packet);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(fragment.transfer());
        buffer.writeVarInt(fragment.index());
        buffer.writeVarInt(fragment.count());
        buffer.writeByteArray(fragment.bytes());
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> receive(fragment)));
        context.get().setPacketHandled(true);
    }

    private static void receive(KnowledgeSnapshotTransfer.Fragment fragment) {
        try {
            CLIENT.accept(fragment).ifPresent(tag -> KnowledgeSyncSnapshot.CODEC.parse(NbtOps.INSTANCE, tag)
                    .resultOrPartial(message -> FRMain.LOGGER.error("Knowledge snapshot: {}", message))
                    .ifPresent(FHKnowledgeDataSyncPacket::installClient));
        } catch (IOException exception) {
            CLIENT.reset();
            FRMain.LOGGER.error("Could not assemble knowledge snapshot", exception);
        }
    }
}
