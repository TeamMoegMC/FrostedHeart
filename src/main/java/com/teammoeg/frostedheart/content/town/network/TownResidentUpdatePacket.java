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
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步居民数据。
 * 包含需要更新（新增/修改）的居民和需要移除的居民UUID。
 */
public class TownResidentUpdatePacket implements CMessage {
    private final Map<UUID, Resident> changed;
    private final Set<UUID> removed;

    public TownResidentUpdatePacket(Map<UUID, Resident> changed, Set<UUID> removed) {
        this.changed = changed;
        this.removed = removed;
    }

    public TownResidentUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed residents: key via native UUID, value via Resident.CODEC
        this.changed = buffer.readMap(
            FriendlyByteBuf::readUUID,
            buf -> { Object o = ObjectWriter.readObject(buf); return CodecUtil.decodeOrThrow(Resident.CODEC.decode(DataOps.COMPRESSED, o)); });
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
            (buf, resident) -> CodecUtil.writeCodec(buf, Resident.CODEC, resident));
        // Write removed UUIDs
        buffer.writeVarInt(removed.size());
        for (UUID uuid : removed) {
            buffer.writeUUID(uuid);
        }
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(townData -> townData.applyResidentUpdate(changed, removed));
        });
        context.get().setPacketHandled(true);
    }
}
