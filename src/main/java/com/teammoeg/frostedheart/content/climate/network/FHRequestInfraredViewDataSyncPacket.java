/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client-carried display baseline and previous analytic-field footprint. */
public record FHRequestInfraredViewDataSyncPacket(
        int requestId, boolean forceFull, long knownGeneration, long knownCenter, int lastEpoch,
        long[] knownPresence, boolean knownReadable, long[] knownFieldPages, long knownStoredEpoch) implements CMessage {
    public static final int PRESENCE_WORDS = 12;

    public FHRequestInfraredViewDataSyncPacket {
        if (knownPresence.length != PRESENCE_WORDS) throw new IllegalArgumentException("infrared presence length");
    }

    public FHRequestInfraredViewDataSyncPacket(FriendlyByteBuf b) {
        this(b.readVarInt(), b.readBoolean(), b.readVarLong(), b.readLong(), b.readVarInt(),
                readPresence(b), b.readBoolean(), readFieldPages(b), b.readVarLong());
    }

    private static long[] readPresence(FriendlyByteBuf b) {
        long[] result = new long[PRESENCE_WORDS];
        for (int i = 0; i < result.length; i++) result[i] = b.readLong();
        return result;
    }

    static long[] readFieldPages(FriendlyByteBuf b) {
        return b.readBoolean() ? readPresence(b) : new long[0];
    }

    static void writeFieldPages(FriendlyByteBuf b, long[] pages) {
        boolean present = false;
        for (long word : pages) present |= word != 0;
        b.writeBoolean(present);
        if (present) for (long word : pages) b.writeLong(word);
    }

    @Override public void encode(FriendlyByteBuf b) {
        b.writeVarInt(requestId);
        b.writeBoolean(forceFull);
        b.writeVarLong(knownGeneration);
        b.writeLong(knownCenter);
        b.writeVarInt(lastEpoch);
        for (long word : knownPresence) b.writeLong(word);
        b.writeBoolean(knownReadable);
        writeFieldPages(b, forceFull ? new long[0] : knownFieldPages);
        b.writeVarLong(knownStoredEpoch);
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            var player = context.get().getSender();
            if (player == null) return;
            var snapshot = MinecraftThermalInput.gameplayInfraredSnapshot(player, forceFull,
                    knownGeneration, knownCenter, lastEpoch, knownPresence, knownReadable,
                    knownFieldPages, knownStoredEpoch);
            if (snapshot != null) {
                int count = Math.max(1, snapshot.brickRecords().length);
                for (int part = 0; part < count; part++)
                    FHNetwork.INSTANCE.sendPlayer(player, new FHResponseInfraredViewDataSyncPacket(requestId, snapshot, part));
            }
        });
        context.get().setPacketHandled(true);
    }
}
