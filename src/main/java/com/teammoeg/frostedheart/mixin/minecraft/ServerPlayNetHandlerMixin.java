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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.AllBlocks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
/**
 * Stop players from being kicked when flying
 * */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetHandlerMixin {
    @Shadow
    boolean clientIsFloating;
    @Shadow
    boolean clientVehicleIsFloating;
    @Shadow
    ServerPlayer player;

    @Inject(at = @At("TAIL"), method = "handleMovePlayer")
    public void fh$processPlayer(ServerboundMovePlayerPacket packetIn, CallbackInfo cbi) {
        if (player.getCommandSenderWorld().getBlockState(player.blockPosition()) == AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState() ||
                player.getCommandSenderWorld().getBlockState(player.blockPosition().above()) == AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
        	clientIsFloating = false;
        }
    }

    @Inject(at = @At("TAIL"), method = "handleMoveVehicle")
    public void fh$processVehicleMove(ServerboundMoveVehiclePacket packetIn, CallbackInfo cbi) {
        if (player.getCommandSenderWorld().getBlockState(player.blockPosition()) == AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState() ||
                player.getCommandSenderWorld().getBlockState(player.blockPosition().above()) == AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
        	clientVehicleIsFloating = false;
        }
    }
}
