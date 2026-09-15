/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.lowdragmc.lowdraglib.LDLib;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** One ordered part of a final display transaction, including field-only displays. */
public final class FHResponseInfraredViewDataSyncPacket implements CMessage {
    private static final int FULL = 1, FIRST = 2, LAST = 4, READABLE = 16;
    private final int requestId;
    private final InfraredSnapshot snapshot;
    private final boolean firstPart, lastPart;

    public FHResponseInfraredViewDataSyncPacket(int requestId, InfraredSnapshot source, int part) {
        this.requestId = requestId;
        firstPart = part == 0;
        lastPart = part == Math.max(1, source.brickRecords().length) - 1;
        snapshot = new InfraredSnapshot(source.centerChunkX(), source.centerChunkZ(), source.centerSectionY(),
                source.generation(), source.infraredEpoch(), source.readable(), source.full(), source.presence(),
                source.fieldPages(), source.brickRecords().length == 0 ? InfraredBrickCodec.NO_PARTS
                        : new byte[][] {source.brickRecords()[part]}, source.storedEpoch(), source.storedSampleTick());
    }

    public FHResponseInfraredViewDataSyncPacket(FriendlyByteBuf b) {
        if (b.readableBytes() > InfraredBrickCodec.MAX_PACKET_BYTES) throw new IllegalArgumentException("infrared packet size");
        requestId = b.readVarInt();
        int x = b.readInt(), z = b.readInt(), y = b.readInt();
        long generation = b.readVarLong();
        int epoch = b.readVarInt(), flags = b.readUnsignedByte();
        firstPart = (flags & FIRST) != 0;
        lastPart = (flags & LAST) != 0;
        long[] presence = new long[0];
        if (b.readBoolean()) {
            presence = new long[FHRequestInfraredViewDataSyncPacket.PRESENCE_WORDS];
            for (int i = 0; i < presence.length; i++) presence[i] = b.readLong();
        }
        long[] fields = FHRequestInfraredViewDataSyncPacket.readFieldPages(b);
        long storedEpoch = b.readVarLong();
        long storedSampleTick = b.readLong();
        byte[][] records = {b.readByteArray(InfraredBrickCodec.MAX_PAYLOAD_BYTES)};
        snapshot = new InfraredSnapshot(x, z, y, generation, epoch,
                (flags & READABLE) != 0, (flags & FULL) != 0, presence, fields, records, storedEpoch, storedSampleTick);
    }
    public InfraredSnapshot snapshot() { return snapshot; }
    public boolean firstPart() { return firstPart; }
    public boolean lastPart() { return lastPart; }

    @Override public void encode(FriendlyByteBuf b) {
        int start = b.writerIndex();
        b.writeVarInt(requestId);
        b.writeInt(snapshot.centerChunkX()); b.writeInt(snapshot.centerChunkZ()); b.writeInt(snapshot.centerSectionY());
        b.writeVarLong(snapshot.generation()); b.writeVarInt(snapshot.infraredEpoch());
        b.writeByte((snapshot.full() ? FULL : 0) | (firstPart ? FIRST : 0) | (lastPart ? LAST : 0)
                | (snapshot.readable() ? READABLE : 0));
        b.writeBoolean(snapshot.presence().length != 0);
        for (long word : snapshot.presence()) b.writeLong(word);
        FHRequestInfraredViewDataSyncPacket.writeFieldPages(b, snapshot.fieldPages());
        b.writeVarLong(snapshot.storedEpoch());
        b.writeLong(snapshot.storedSampleTick());
        b.writeByteArray(snapshot.brickRecords().length == 0 ? new byte[0] : snapshot.brickRecords()[0]);
        if (b.writerIndex() - start > InfraredBrickCodec.MAX_PACKET_BYTES)
            throw new IllegalArgumentException("infrared packet exceeded wire budget");
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!LDLib.isClient()) return;
            Runnable update = () -> InfraredViewRenderer.updateData(requestId, snapshot, firstPart, lastPart);
            if (RenderSystem.isOnRenderThread()) update.run();
            else RenderSystem.recordRenderCall(update::run);
        });
        context.get().setPacketHandled(true);
    }
}
