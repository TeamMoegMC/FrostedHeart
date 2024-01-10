package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import com.simibubi.create.AllBlocks;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.network.play.client.CPlayerPacket;
@Mixin(ServerPlayNetHandler.class)
public class ServerPlayNetHandlerMixin {
	@Shadow
	boolean floating;
	@Shadow
	boolean vehicleFloating;
	@Shadow
	ServerPlayerEntity player;
	@Inject(at=@At("TAIL"),method="processVehicleMove(Lnet/minecraft/network/play/client/CMoveVehiclePacket;)V")
	public void fh$processVehicleMove(CMoveVehiclePacket packetIn) {
		if(player.getEntityWorld().getBlockState(player.getPosition())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()||
				player.getEntityWorld().getBlockState(player.getPosition().up())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
			vehicleFloating=false;
		}
	}
	@Inject(at=@At("TAIL"),method="processPlayer(Lnet/minecraft/network/play/client/CPlayerPacket;)V")
	public void fh$processPlayer(CPlayerPacket packetIn) {
		if(player.getEntityWorld().getBlockState(player.getPosition())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()||
				player.getEntityWorld().getBlockState(player.getPosition().up())==AllBlocks.CRUSHING_WHEEL_CONTROLLER.getDefaultState()) {
			floating=false;
		}
	}
}
