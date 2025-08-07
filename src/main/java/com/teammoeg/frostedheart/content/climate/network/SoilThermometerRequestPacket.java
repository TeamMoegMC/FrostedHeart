package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoilThermometerRequestPacket(BlockPos pos) implements CMessage {

    public SoilThermometerRequestPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        context.get().enqueueWork(() ->
            FHNetwork.INSTANCE.sendPlayer(player, new SoilThermometerUpdatePacket(WorldTemperature.block(player.level(), pos)))
        );
        context.get().setPacketHandled(true);
    }
}
