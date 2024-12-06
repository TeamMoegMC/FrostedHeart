package com.teammoeg.frostedheart.content.health.network;

import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncHandler {

    public static void handle(Supplier<NetworkEvent.Context> context, PlayerNutritionSyncPacket packet){
        context.get().enqueueWork(() -> NutritionCapability.getCapability(Minecraft.getInstance().player).ifPresent(data -> {
            data.setCarbohydrate(packet.carbohydrate);
            data.setProtein(packet.protein);
            data.setVegetable(packet.vegetable);
            data.setFat(packet.fat);
        }));
        context.get().setPacketHandled(true);

    }
}
