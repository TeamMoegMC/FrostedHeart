/*
 * Copyright (c) 2026 TeamMoeg
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

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import com.teammoeg.frostedheart.content.water.util.WaterLevelUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

public record PlayerDrinkWaterMessage(BlockPos drinkingLocation) implements CMessage {
	
    public PlayerDrinkWaterMessage(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(drinkingLocation);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        
       context.get().enqueueWork(()->{
    	   ServerPlayer player = context.get().getSender();
    	   ServerLevel level=player.serverLevel();
    	   if(level.isLoaded(drinkingLocation)&&drinkingLocation.distSqr(player.blockPosition())<=36) {
    		   if(WaterLevelUtil.drink(player,level.getFluidState(drinkingLocation).getType())) {
    			   player.playSound(SoundEvents.GENERIC_DRINK,.4f,1f);
	        	   WaterLevelCapability.getCapability(player).ifPresent(data -> {
	                   FHNetwork.INSTANCE.sendPlayer( (ServerPlayer) player, new PlayerWaterLevelSyncPacket(data.getWaterLevel(), data.getWaterSaturationLevel(), data.getWaterExhaustionLevel()));
	               });
    		   }
    	   }
       });
       context.get().setPacketHandled(true);
        
       
    }
}
