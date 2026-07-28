package com.teammoeg.frostedheart.content.town.network;

import java.util.*;
import java.util.function.Supplier;

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
    private final Map<BlockPos, ITownBuilding> changed;
    private final Set<BlockPos> removed;

    public TownBuildingUpdatePacket(Map<BlockPos, ITownBuilding> changed, Set<BlockPos> removed) {
        this.changed = changed;
        this.removed = removed;
    }

    public TownBuildingUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed buildings: key via native BlockPos, value via ITownBuilding.CODEC
        this.changed = buffer.readMap(
            FriendlyByteBuf::readBlockPos,
            buf -> { Object o = ObjectWriter.readObject(buf); return CodecUtil.decodeOrThrow(ITownBuilding.CODEC.decode(DataOps.COMPRESSED, o)); });
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
            (buf, building) -> CodecUtil.writeCodec(buf, ITownBuilding.CODEC, building));
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
                .ifPresent(townData -> townData.applyBuildingUpdate(changed, removed));
        });
        context.get().setPacketHandled(true);
    }
}
