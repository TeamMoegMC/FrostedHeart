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
import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步建筑数据。
 * 包含需要更新（新增/修改）的建筑和需要移除的建筑位置。
 */
public class TownBuildingUpdatePacket implements TownViewPacket {
    private final Map<BlockPos, Object> changed;
    private final Set<BlockPos> removed;

    public TownBuildingUpdatePacket(Map<BlockPos, ITownBuilding> changed, Set<BlockPos> removed) {
        this.changed = new java.util.LinkedHashMap<>();
        changed.forEach((key, value) -> this.changed.put(key, CodecUtil.encodeOrThrow(ITownBuilding.CODEC.encodeStart(DataOps.COMPRESSED, value))));
        this.removed = Set.copyOf(removed);
    }

    public TownBuildingUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed buildings: key via native BlockPos, value via ITownBuilding.CODEC
        this.changed = buffer.readMap(
            FriendlyByteBuf::readBlockPos,
            ObjectWriter::readObject);
        // Read removed positions
        int removedSize = buffer.readVarInt();
        this.removed = new HashSet<>(removedSize);
        for (int i = 0; i < removedSize; i++) {
            this.removed.add(buffer.readBlockPos());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed buildings: key via native BlockPos, value via ITownBuilding.CODEC
        buffer.writeMap(changed,
            FriendlyByteBuf::writeBlockPos,
            ObjectWriter::writeObject);
        // Write removed positions
        buffer.writeVarInt(removed.size());
        for (BlockPos pos : removed) {
            buffer.writeBlockPos(pos);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!com.teammoeg.frostedheart.content.town.client.TownClientUpdates.canApply(context.get().getNetworkManager())) return;
            com.teammoeg.frostedheart.content.town.client.TownClientUpdates.apply(townData -> {
                    Map<BlockPos, ITownBuilding> decoded = new java.util.LinkedHashMap<>();
                    changed.forEach((key, value) -> decoded.put(key, CodecUtil.decodeOrThrow(ITownBuilding.CODEC.decode(DataOps.COMPRESSED, value))));
                    townData.applyBuildingUpdate(decoded, removed);
                });
        });
        context.get().setPacketHandled(true);
    }
}
