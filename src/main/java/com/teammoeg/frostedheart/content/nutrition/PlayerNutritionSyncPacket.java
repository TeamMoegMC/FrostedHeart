package com.teammoeg.frostedheart.content.nutrition;

import com.teammoeg.frostedheart.base.network.NBTMessage;
import com.teammoeg.frostedheart.content.water.WaterLevelCapability;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncPacket extends NBTMessage {
     float fruit ,grain ,portein,vegetable,sugar = 1.0f;

     public PlayerNutritionSyncPacket(float fruit ,float grain ,float portein,float vegetable,float sugar) {
         super(toTag(fruit, grain, portein, vegetable, sugar));
     }


    public PlayerNutritionSyncPacket(Player pe) {
        super(NutritionCapability.getCapability(pe).map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new));
    }

    public static CompoundTag toTag(float fruit ,float grain ,float portein,float vegetable,float sugar) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("fruit", fruit);
        compound.putFloat("grain", grain);
        compound.putFloat("protein", portein);
        compound.putFloat("vegetable", vegetable);
        compound.putFloat("sugar", sugar);
        return compound;
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.get().enqueueWork(() -> NutritionCapability.getCapability(Minecraft.getInstance().player).ifPresent(date -> {
                date.setFruit(fruit);
                date.setGrain(grain);
                date.setProtein(portein);
                date.setVegetable(vegetable);
                date.setSugar(sugar);
            }));
        }
    }
}
