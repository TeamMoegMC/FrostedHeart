package com.teammoeg.frostedheart.content.town.network;

import java.util.UUID;
import java.util.function.Supplier;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Pull only while the zone overlay is open. Unchanged versions produce no response. */
public final class TownRoadViewRequestPacket implements CMessage {
    private final UUID knownVersion;
    public TownRoadViewRequestPacket(UUID version) { knownVersion = version; }
    public TownRoadViewRequestPacket(FriendlyByteBuf buf) { knownVersion = buf.readUUID(); }
    public void encode(FriendlyByteBuf buf) { buf.writeUUID(knownVersion); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;
            var holder = CTeamDataManager.get(player);
            holder.getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(town -> {
                var dimension = TeamTownData.resolveTownDimension(holder);
                if (dimension == null || !dimension.equals(player.level().dimension())) return;
                var connectivity = town.connectivityView();
                if (!connectivity.version().equals(knownVersion)) FHNetwork.INSTANCE.sendPlayer(player,
                        new TownRoadViewPacket(holder.getId(), dimension.location(), connectivity.version(), connectivity.view()));
            });
        });
        ctx.setPacketHandled(true);
    }
}
