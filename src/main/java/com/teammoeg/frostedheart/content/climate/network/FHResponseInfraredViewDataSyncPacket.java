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
    private static final int FULL = 1, FIRST = 2, LAST = 4, ACK = 8, READABLE = 16;
    private final int requestId;
    private final InfraredSnapshot snapshot;
    private final boolean firstPart, lastPart;
    private final boolean acknowledgement;

    public FHResponseInfraredViewDataSyncPacket(int requestId, InfraredSnapshot source, int part) {
        this.requestId = requestId;
        acknowledgement = false;
        firstPart = part == 0;
        lastPart = part == Math.max(1, source.brickRecords().length) - 1;
        snapshot = new InfraredSnapshot(source.centerChunkX(), source.centerChunkZ(), source.centerSectionY(),
                source.generation(), source.infraredEpoch(), source.readable(), source.full(), source.presence(),
                source.fieldPages(), source.brickRecords().length == 0 ? InfraredBrickCodec.NO_PARTS
                        : new byte[][] {source.brickRecords()[part]}, source.storedEpoch(), source.storedSampleTick());
    }

    public static FHResponseInfraredViewDataSyncPacket acknowledge(FHRequestInfraredViewDataSyncPacket request) {
        return new FHResponseInfraredViewDataSyncPacket(request.requestId());
    }

    public static FHResponseInfraredViewDataSyncPacket acknowledge(int requestId) {
        return new FHResponseInfraredViewDataSyncPacket(requestId);
    }

    private FHResponseInfraredViewDataSyncPacket(int requestId) {
        this.requestId = requestId;
        acknowledgement = firstPart = lastPart = true;
        snapshot = null;
    }

    public FHResponseInfraredViewDataSyncPacket(FriendlyByteBuf b) {
        if (b.readableBytes() > InfraredBrickCodec.MAX_PACKET_BYTES) throw new IllegalArgumentException("infrared packet size");
        requestId = b.readVarInt();
        int flags = b.readUnsignedByte();
        acknowledgement = (flags & ACK) != 0;
        firstPart = (flags & FIRST) != 0;
        lastPart = (flags & LAST) != 0;
        if (acknowledgement) { snapshot = null; return; }
        int x = b.readInt(), z = b.readInt(), y = b.readInt();
        long generation = b.readVarLong();
        int epoch = b.readVarInt();
        long[] presence = new long[0];
        long[] fields = new long[0];
        long storedEpoch = 0, storedSampleTick = 0;
        if (lastPart) {
            if (b.readBoolean()) presence = FHRequestInfraredViewDataSyncPacket.readPresence(b);
            fields = FHRequestInfraredViewDataSyncPacket.readFieldPages(b);
            storedEpoch = b.readVarLong();
            storedSampleTick = b.readLong();
        }
        byte[][] records = {b.readByteArray(InfraredBrickCodec.MAX_PAYLOAD_BYTES)};
        snapshot = new InfraredSnapshot(x, z, y, generation, epoch,
                (flags & READABLE) != 0, (flags & FULL) != 0, presence, fields, records, storedEpoch, storedSampleTick);
    }
    public InfraredSnapshot snapshot() { return snapshot; }
    public int requestId() { return requestId; }
    public boolean isAcknowledgement() { return acknowledgement; }
    public boolean firstPart() { return firstPart; }
    public boolean lastPart() { return lastPart; }

    @Override public void encode(FriendlyByteBuf b) {
        int start = b.writerIndex();
        b.writeVarInt(requestId);
        int flags = (firstPart ? FIRST : 0) | (lastPart ? LAST : 0) | (acknowledgement ? ACK : 0);
        if (!acknowledgement) flags |= (snapshot.full() ? FULL : 0) | (snapshot.readable() ? READABLE : 0);
        b.writeByte(flags);
        if (acknowledgement) return;
        b.writeInt(snapshot.centerChunkX()); b.writeInt(snapshot.centerChunkZ()); b.writeInt(snapshot.centerSectionY());
        b.writeVarLong(snapshot.generation()); b.writeVarInt(snapshot.infraredEpoch());
        if (lastPart) {
            b.writeBoolean(snapshot.presence().length != 0);
            if (snapshot.presence().length != 0) FHRequestInfraredViewDataSyncPacket.writeFieldPages(b, snapshot.presence());
            FHRequestInfraredViewDataSyncPacket.writeFieldPages(b, snapshot.fieldPages());
            b.writeVarLong(snapshot.storedEpoch());
            b.writeLong(snapshot.storedSampleTick());
        }
        b.writeByteArray(snapshot.brickRecords().length == 0 ? new byte[0] : snapshot.brickRecords()[0]);
        if (b.writerIndex() - start > InfraredBrickCodec.MAX_PACKET_BYTES)
            throw new IllegalArgumentException("infrared packet exceeded wire budget");
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!LDLib.isClient()) return;
            Runnable update = () -> {
                if (acknowledgement) InfraredViewRenderer.acknowledgeRequest(requestId);
                else InfraredViewRenderer.updateData(requestId, snapshot, firstPart, lastPart);
            };
            if (RenderSystem.isOnRenderThread()) update.run();
            else RenderSystem.recordRenderCall(update::run);
        });
        context.get().setPacketHandled(true);
    }
}
