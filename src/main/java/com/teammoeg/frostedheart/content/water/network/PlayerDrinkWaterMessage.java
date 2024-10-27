package com.teammoeg.frostedheart.content.water.network;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.base.network.NBTMessage;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.water.event.WaterEventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PlayerDrinkWaterMessage extends NBTMessage {


    public PlayerDrinkWaterMessage(FriendlyByteBuf buffer) {
        super(buffer);
    }
    public PlayerDrinkWaterMessage() {
        super(new CompoundTag());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        Player player = context.get().getSender();
        WaterEventHandler.drinkWaterBlock(player);
        WaterLevelCapability.getCapability(player).ifPresent(data -> {
            FHNetwork.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new PlayerWaterLevelSyncPacket(data.getWaterLevel(), data.getWaterSaturationLevel(), data.getWaterExhaustionLevel()));
        });
    }
}
