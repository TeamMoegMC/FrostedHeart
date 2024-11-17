package com.teammoeg.frostedheart.content.nutrition.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.nutrition.capability.NutritionCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncPacket implements FHMessage {
    private float fat , carbohydrate, protein,vegetable;

    public PlayerNutritionSyncPacket(FriendlyByteBuf buffer) {
        fat = buffer.readFloat();
        carbohydrate = buffer.readFloat();
        protein = buffer.readFloat();
        vegetable = buffer.readFloat();
    }

     public PlayerNutritionSyncPacket(float fat ,float carbohydrate ,float portein,float vegetable) {
        this.fat = fat;
        this.carbohydrate = carbohydrate;
        this.protein = portein;
        this.vegetable = vegetable;
     }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(fat);
        buffer.writeFloat(carbohydrate);
        buffer.writeFloat(protein);
        buffer.writeFloat(vegetable);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.get().enqueueWork(() -> NutritionCapability.getCapability(Minecraft.getInstance().player).ifPresent(date -> {
                date.setCarbohydrate(carbohydrate);
                date.setProtein(protein);
                date.setVegetable(vegetable);
                date.setFat(fat);
            }));
        }
    }
}
