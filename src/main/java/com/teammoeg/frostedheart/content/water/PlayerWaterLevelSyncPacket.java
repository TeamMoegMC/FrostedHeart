package com.teammoeg.frostedheart.content.water;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.base.network.NBTMessage;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerWaterLevelSyncPacket extends NBTMessage {
    int waterLevel, waterSaturationLevel;
    float waterExhaustionLevel;

    public PlayerWaterLevelSyncPacket(int waterLevel, int waterSaturationLevel, float waterExhaustionLevel) {
        super(toTag(waterLevel, waterSaturationLevel, waterExhaustionLevel));
    }


    public PlayerWaterLevelSyncPacket(Player pe) {
        super(WaterLevelCapability.getCapability(pe).map(NBTSerializable::serializeNBT).orElseGet(CompoundTag::new));
    }

    public static CompoundTag toTag(int waterLevel, int waterSaturationLevel, float waterExhaustionLevel) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("PlayerWaterLevel", waterLevel);
        compound.putInt("PlayerWaterSaturationLevel", waterSaturationLevel);
        compound.putFloat("PlayerWaterExhaustionLevel", waterExhaustionLevel);
        return compound;
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        if (context.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.get().enqueueWork(() -> WaterLevelCapability.getCapability(Minecraft.getInstance().player).ifPresent(date -> {
                date.setWaterSaturationLevel(waterSaturationLevel);
                date.setWaterLevel(waterLevel);
                date.setWaterExhaustionLevel(waterExhaustionLevel);
            }));
        }
    }
}
