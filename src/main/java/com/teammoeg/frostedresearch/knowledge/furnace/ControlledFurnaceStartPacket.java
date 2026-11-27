package com.teammoeg.frostedresearch.knowledge.furnace;

import com.teammoeg.chorda.network.CMessage;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ControlledFurnaceStartPacket(int containerId, BlockPos pos, int temperature, int seconds) implements CMessage {
    public ControlledFurnaceStartPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(temperature);
        buffer.writeVarInt(seconds);
    }

    @Override public void handle(Supplier<NetworkEvent.Context> context) {
        var network = context.get();
        network.enqueueWork(() -> {
            var player = network.getSender();
            if (player != null) apply(player);
        });
        network.setPacketHandled(true);
    }

    public void apply(ServerPlayer player) {
        if (player.containerMenu instanceof ControlledFurnaceMenu menu && menu.containerId == containerId
                && menu.getBlock().getBlockPos().equals(pos)) menu.start(player, temperature, seconds);
    }
}
