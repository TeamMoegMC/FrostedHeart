package com.teammoeg.frostedheart.content.nutrition.network;

import com.teammoeg.frostedheart.base.network.NBTMessage;
import com.teammoeg.frostedheart.content.nutrition.capability.NutritionCapability;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncPacket extends NBTMessage {
    private float vitamin , carbohydrate, protein,vegetable;

     public PlayerNutritionSyncPacket(float vitamin ,float carbohydrate ,float portein,float vegetable) {
         super(toTag(vitamin, carbohydrate, portein, vegetable));
     }


    public PlayerNutritionSyncPacket(Player pe) {
        super(NutritionCapability.getCapability(pe).map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new));
    }

    public static CompoundTag toTag(float vitamin ,float carbohydrate ,float portein,float vegetable) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("vitamin", vitamin);
        compound.putFloat("carbohydrate", carbohydrate);
        compound.putFloat("protein", portein);
        compound.putFloat("vegetable", vegetable);
        return compound;
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.get().enqueueWork(() -> NutritionCapability.getCapability(Minecraft.getInstance().player).ifPresent(date -> {
                date.setCarbohydrate(carbohydrate);
                date.setProtein(protein);
                date.setVegetable(vegetable);
                date.setVitamin(vitamin);
            }));
        }
    }
}
