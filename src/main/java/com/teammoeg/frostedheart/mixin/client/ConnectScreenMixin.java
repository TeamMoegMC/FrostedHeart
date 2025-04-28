package com.teammoeg.frostedheart.mixin.client;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

	public ConnectScreenMixin() {

	}

	@Inject(at = @At("HEAD"), method = "connect")
	private void fh$connect(final Minecraft pMinecraft, final ServerAddress pServerAddress, @Nullable final ServerData pServerData, CallbackInfo cbi) {
		ClientConnectionHelper.last=pServerAddress;
		if(ClientUtils.mc().screen instanceof JoinMultiplayerScreen)
			ClientConnectionHelper.callStack.clear();
	}
}
