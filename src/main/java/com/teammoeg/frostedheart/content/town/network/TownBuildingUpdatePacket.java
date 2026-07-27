package com.teammoeg.frostedheart.content.town.network;

import java.util.*;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.building.ITownBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步建筑数据。
 * 包含需要更新（新增/修改）的建筑和需要移除的建筑位置。
 */
public class TownBuildingUpdatePacket implements CMessage {
    private static final Codec<Map<BlockPos, ITownBuilding>> BUILDINGS_CODEC =
        CodecUtil.mapCodec("pos", BlockPos.CODEC, "building", ITownBuilding.CODEC);

    private final Map<BlockPos, ITownBuilding> changed;
    private final Set<BlockPos> removed;

    public TownBuildingUpdatePacket(Map<BlockPos, ITownBuilding> changed, Set<BlockPos> removed) {
        this.changed = changed;
        this.removed = removed;
    }

    public TownBuildingUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed buildings via codec
        Object data = ObjectWriter.readObject(buffer);
        this.changed = CodecUtil.decodeOrThrow(BUILDINGS_CODEC.decode(DataOps.COMPRESSED, data));
        // Read removed positions
        int removedSize = buffer.readVarInt();
        this.removed = new HashSet<>(removedSize);
        for (int i = 0; i < removedSize; i++) {
            this.removed.add(buffer.readBlockPos());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed buildings via codec
        Object data = CodecUtil.encodeOrThrow(BUILDINGS_CODEC.encodeStart(DataOps.COMPRESSED, changed));
        ObjectWriter.writeObject(buffer, data);
        // Write removed positions
        buffer.writeVarInt(removed.size());
        for (BlockPos pos : removed) {
            buffer.writeBlockPos(pos);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                ;//todo .ifPresent(townData -> townData.applyBuildingUpdate(changed, removed));
        });
        context.get().setPacketHandled(true);
    }
}
