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
import com.teammoeg.frostedheart.content.town.resident.Resident;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步居民数据。
 * 包含需要更新（新增/修改）的居民和需要移除的居民UUID。
 */
public class TownResidentUpdatePacket implements CMessage {
    private static final Codec<Map<UUID, Resident>> RESIDENTS_CODEC =
        CodecUtil.mapCodec("uuid", UUIDUtil.CODEC, "data", Resident.CODEC);

    private final Map<UUID, Resident> changed;
    private final Set<UUID> removed;

    public TownResidentUpdatePacket(Map<UUID, Resident> changed, Set<UUID> removed) {
        this.changed = changed;
        this.removed = removed;
    }

    public TownResidentUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed residents via codec
        Object data = ObjectWriter.readObject(buffer);
        this.changed = CodecUtil.decodeOrThrow(RESIDENTS_CODEC.decode(DataOps.COMPRESSED, data));
        // Read removed UUIDs
        int removedSize = buffer.readVarInt();
        this.removed = new HashSet<>(removedSize);
        for (int i = 0; i < removedSize; i++) {
            this.removed.add(buffer.readUUID());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed residents via codec
        Object data = CodecUtil.encodeOrThrow(RESIDENTS_CODEC.encodeStart(DataOps.COMPRESSED, changed));
        ObjectWriter.writeObject(buffer, data);
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
                ;//todo .ifPresent(townData -> townData.applyResidentUpdate(changed, removed));
        });
        context.get().setPacketHandled(true);
    }
}
