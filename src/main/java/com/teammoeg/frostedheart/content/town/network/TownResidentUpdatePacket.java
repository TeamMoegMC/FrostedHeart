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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步居民数据。
 * 包含需要更新（新增/修改）的居民和需要移除的居民UUID。
 */
public class TownResidentUpdatePacket implements TownViewPacket {
    private final Map<UUID, Object> changed;
    private final Set<UUID> removed;

    public TownResidentUpdatePacket(Map<UUID, Resident> changed, Set<UUID> removed) {
        this.changed = new java.util.LinkedHashMap<>();
        changed.forEach((key, value) -> this.changed.put(key, CodecUtil.encodeOrThrow(Resident.CODEC.encodeStart(DataOps.COMPRESSED, value))));
        this.removed = Set.copyOf(removed);
    }

    public TownResidentUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed residents: key via native UUID, value via Resident.CODEC
        this.changed = buffer.readMap(
            FriendlyByteBuf::readUUID,
            ObjectWriter::readObject);
        // Read removed UUIDs
        int removedSize = buffer.readVarInt();
        this.removed = new HashSet<>(removedSize);
        for (int i = 0; i < removedSize; i++) {
            this.removed.add(buffer.readUUID());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed residents: key via native UUID, value via Resident.CODEC
        buffer.writeMap(changed,
            FriendlyByteBuf::writeUUID,
            ObjectWriter::writeObject);
        // Write removed UUIDs
        buffer.writeVarInt(removed.size());
        for (UUID uuid : removed) {
            buffer.writeUUID(uuid);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!com.teammoeg.frostedheart.content.town.client.TownClientUpdates.canApply(context.get().getNetworkManager())) return;
            com.teammoeg.frostedheart.content.town.client.TownClientUpdates.apply(townData -> {
                    Map<UUID, Resident> decoded = new java.util.LinkedHashMap<>();
                    changed.forEach((key, value) -> decoded.put(key, CodecUtil.decodeOrThrow(Resident.CODEC.decode(DataOps.COMPRESSED, value))));
                    townData.applyResidentUpdate(decoded, removed);
                });
        });
        context.get().setPacketHandled(true);
    }
}
