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
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步资源数据。
 * 包含发生变化的资源key及其当前数量，以及最新的已占用容量。
 */
public class TownResourceUpdatePacket implements CMessage {
    private static final Codec<Map<ITownResourceKey, Double>> RESOURCES_CODEC =
        CodecUtil.mapCodec("resourceKey", ITownResourceKey.CODEC, "amount", Codec.DOUBLE);

    private final Map<ITownResourceKey, Double> changes;
    private final double occupiedCapacity;

    public TownResourceUpdatePacket(Map<ITownResourceKey, Double> changes, double occupiedCapacity) {
        this.changes = changes;
        this.occupiedCapacity = occupiedCapacity;
    }

    public TownResourceUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed resources via codec
        Object data = ObjectWriter.readObject(buffer);
        this.changes = CodecUtil.decodeOrThrow(RESOURCES_CODEC.decode(DataOps.COMPRESSED, data));
        // Read occupied capacity
        this.occupiedCapacity = buffer.readDouble();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed resources via codec
        Object data = CodecUtil.encodeOrThrow(RESOURCES_CODEC.encodeStart(DataOps.COMPRESSED, changes));
        ObjectWriter.writeObject(buffer, data);
        // Write occupied capacity
        buffer.writeDouble(occupiedCapacity);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                ;//todo .ifPresent(townData -> townData.applyResourceUpdate(changes, occupiedCapacity));
        });
        context.get().setPacketHandled(true);
    }
}
