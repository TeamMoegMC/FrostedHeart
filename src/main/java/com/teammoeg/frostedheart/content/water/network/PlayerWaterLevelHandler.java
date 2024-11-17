package com.teammoeg.frostedheart.content.water.network;

import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerWaterLevelHandler {

    public static void handle(Supplier<NetworkEvent.Context> context,PlayerWaterLevelSyncPacket packet){
        context.get().enqueueWork(() -> WaterLevelCapability.getCapability(Minecraft.getInstance().player).ifPresent(data -> {
            data.setWaterSaturationLevel(packet.waterSaturationLevel);
            data.setWaterLevel(packet.waterLevel);
            data.setWaterExhaustionLevel(packet.waterExhaustionLevel);
        }));
    }
}
