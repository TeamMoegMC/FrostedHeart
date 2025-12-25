package com.teammoeg.chorda.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.chorda.events.ServerLevelDataSaveEvent;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

	public MinecraftServerMixin() {
	
	}
	@Inject(at=@At("HEAD"),method="saveAllChunks")
	public void saveAllChunks(boolean pSuppressLog, boolean pFlush, boolean pForced,CallbackInfoReturnable<Boolean> ret) {
		MinecraftForge.EVENT_BUS.post(new ServerLevelDataSaveEvent());
	}

}
