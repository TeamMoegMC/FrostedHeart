package com.teammoeg.frostedheart.content.health.network;

import com.teammoeg.frostedheart.base.network.FHMessage;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncPacket implements FHMessage {
    public float fat , carbohydrate, protein,vegetable;

    public PlayerNutritionSyncPacket(FriendlyByteBuf buffer) {
        fat = buffer.readFloat();
        carbohydrate = buffer.readFloat();
        protein = buffer.readFloat();
        vegetable = buffer.readFloat();
    }

    public PlayerNutritionSyncPacket(NutritionCapability.Nutrition nutrition) {
        this(nutrition.fat(),nutrition.carbohydrate(),nutrition.protein(),nutrition.vegetable());
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
            PlayerNutritionSyncHandler.handle(context, this);
        }
    }

    public NutritionCapability.Nutrition getNutrition(){
        return new NutritionCapability.Nutrition(fat,carbohydrate,protein,vegetable);
    }
}
