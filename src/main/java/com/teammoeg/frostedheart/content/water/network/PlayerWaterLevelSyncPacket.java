package com.teammoeg.frostedheart.content.water.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerWaterLevelSyncPacket implements FHMessage {
    int waterLevel, waterSaturationLevel;
    float waterExhaustionLevel;

    public PlayerWaterLevelSyncPacket(FriendlyByteBuf buffer) {
        waterLevel = buffer.readInt();
        waterSaturationLevel = buffer.readInt();
        waterExhaustionLevel = buffer.readFloat();
    }

    public PlayerWaterLevelSyncPacket(int waterLevel, int waterSaturationLevel, float waterExhaustionLevel) {
        this.waterLevel = waterLevel;
        this.waterSaturationLevel = waterSaturationLevel;
        this.waterExhaustionLevel = waterExhaustionLevel;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(waterLevel);
        buffer.writeInt(waterSaturationLevel);
        buffer.writeFloat(waterExhaustionLevel);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.get().enqueueWork(() -> WaterLevelCapability.getCapability(context.get().getSender()).ifPresent(date -> {
                date.setWaterSaturationLevel(waterSaturationLevel);
                date.setWaterLevel(waterLevel);
                date.setWaterExhaustionLevel(waterExhaustionLevel);
            }));
        }
    }
}
