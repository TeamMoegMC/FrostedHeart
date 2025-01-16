package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.chorda.network.CMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeatNetworkRequestC2SPacket implements CMessage {

    private final BlockPos pos; // the position of the network constituent (pipe or endpoint) player looking at


    public HeatNetworkRequestC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public HeatNetworkRequestC2SPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Get the data needed on server side
            var player = context.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof HeatNetworkProvider hp) {
                    HeatNetwork network = hp.getNetwork();
                    if (network != null) {
//                        FHMain.LOGGER.debug("Client request received. Sending server HeatNetwork data to client");
                        ClientHeatNetworkData data = new ClientHeatNetworkData(pos, network);
                        FHNetwork.sendPlayer(player, new HeatNetworkResponseS2CPacket(data));
                    } else {
//                        FHMain.LOGGER.debug("Client request received. No HeatNetwork found at the position. Sending nothing.");
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
