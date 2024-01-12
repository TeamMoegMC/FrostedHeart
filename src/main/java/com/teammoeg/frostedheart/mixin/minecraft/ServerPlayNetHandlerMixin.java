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

package com.teammoeg.frostedheart.mixin.minecraft;

import com.simibubi.create.AllBlocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayNetHandler.class)
public class ServerPlayNetHandlerMixin {
	@Shadow
	boolean floating;
	@Shadow
	boolean vehicleFloating;
	@Shadow
	ServerPlayerEntity player;
	@Inject(at=@At("TAIL"),method="processVehicleMove(Lnet/minecraft/network/play/client/CMoveVehiclePacket;)V")
	public void fh$processVehicleMove(CMoveVehiclePacket packetIn,CallbackInfo cbi) {
		if(player.getEntityWorld().getBlockState(player.getPosition())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()||
				player.getEntityWorld().getBlockState(player.getPosition().up())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
			vehicleFloating=false;
		}
	}
	@Inject(at=@At("TAIL"),method="processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V")
	public void fh$processPlayer(CPlayerPacket packetIn,CallbackInfo cbi) {
		if(player.getEntityWorld().getBlockState(player.getPosition())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()||
				player.getEntityWorld().getBlockState(player.getPosition().up())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
			floating=false;
		}
	}
}
