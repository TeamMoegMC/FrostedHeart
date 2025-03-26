/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.water.network;

import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.chorda.network.NBTMessage;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.water.event.WaterCommonEvents;
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
        buffer.writeNbt(new CompoundTag());
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        Player player = context.get().getSender();
        WaterCommonEvents.drinkWaterBlock(player);
        WaterLevelCapability.getCapability(player).ifPresent(data -> {
            FHNetwork.INSTANCE.sendPlayer( (ServerPlayer) player, new PlayerWaterLevelSyncPacket(data.getWaterLevel(), data.getWaterSaturationLevel(), data.getWaterExhaustionLevel()));
        });
    }
}
