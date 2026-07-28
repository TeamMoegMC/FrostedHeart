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
import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 → 客户端：增量同步资源数据。
 * 包含发生变化的资源key及其当前数量，以及最新的已占用容量。
 */
public class TownResourceUpdatePacket implements CMessage {
    private final Map<ITownResourceKey, Double> changes;
    private final double occupiedCapacity;

    public TownResourceUpdatePacket(Map<ITownResourceKey, Double> changes, double occupiedCapacity) {
        this.changes = changes;
        this.occupiedCapacity = occupiedCapacity;
    }

    public TownResourceUpdatePacket(FriendlyByteBuf buffer) {
        // Read changed resources: key via ITownResourceKey.CODEC, value via native double
        this.changes = buffer.readMap(
            buf -> { Object o = ObjectWriter.readObject(buf); return CodecUtil.decodeOrThrow(ITownResourceKey.CODEC.decode(DataOps.COMPRESSED, o)); },
            FriendlyByteBuf::readDouble);
        // Read occupied capacity
        this.occupiedCapacity = buffer.readDouble();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        // Write changed resources: key via ITownResourceKey.CODEC, value via native double
        buffer.writeMap(changes,
            (buf, key) -> CodecUtil.writeCodec(buf, ITownResourceKey.CODEC, key),
            FriendlyByteBuf::writeDouble);
        // Write occupied capacity
        buffer.writeDouble(occupiedCapacity);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(townData -> townData.applyResourceUpdate(changes, occupiedCapacity));
        });
        context.get().setPacketHandled(true);
    }
}
