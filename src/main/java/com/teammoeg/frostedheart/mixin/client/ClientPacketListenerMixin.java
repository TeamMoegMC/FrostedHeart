package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	public ClientPacketListenerMixin() {

	}

	@Inject(at = @At("HEAD"), method = "setTitleText",cancellable=true)
	public void fh$setTitleText(ClientboundSetTitleTextPacket pPacket, CallbackInfo cbi) {
		String text=pPacket.getText().getString();
		if(text.startsWith(ServerConnectionHelper.HEADER)) {//start mark
			int code=text.codePointAt(2)&0xFF;
			switch(code) {
			case 0:ClientConnectionHelper.token=text.substring(3);break;
			case 1:ClientConnectionHelper.back();break;
			case 2:ClientConnectionHelper.joinNewServer(text.substring(3), false);break;
			case 3:ClientConnectionHelper.joinNewServer(text.substring(3), true);break;
			}
			cbi.cancel();
		}
	}
}
