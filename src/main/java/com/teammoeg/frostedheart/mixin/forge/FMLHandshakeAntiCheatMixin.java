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

package com.teammoeg.frostedheart.mixin.forge;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.network.HandshakeHandler;
import net.minecraftforge.network.HandshakeMessages;
import net.minecraftforge.network.NetworkEvent;

@Mixin(HandshakeHandler.class)
public class FMLHandshakeAntiCheatMixin {
	@Inject(at = @At("HEAD"), method = "handleClientModListOnServer", remap = false,cancellable=true)
	void fh$handleClientModListOnServer(HandshakeMessages.C2SModListReply clientModList,
			Supplier<NetworkEvent.Context> c, CallbackInfo cbi) {
		Set<String> cli=clientModList.getModList().stream().map(String::toLowerCase).collect(Collectors.toSet());
		for(String s:FHConfig.COMMON.blackmods.get()) {
			if(cli.contains(s)) {
				c.get().setPacketHandled(true);
				FHMain.LOGGER.warn("Rejected Connection: Blacklisted mods ");
				Component t=Component.translatable("message.frostedheart.disallowed_mods");
				c.get().getNetworkManager().disconnect(t);
				
				cbi.cancel();
				return;
			}
		}
		
		
		if(ServerConnectionHelper.isAuthEnabled) {
			NetworkEvent.Context ctx=c.get();
			try {
				String token=ServerConnectionHelper.currentToken.get();
				if(token!=null) {
					String decoded=ServerConnectionHelper.decode(token);
					FHMain.LOGGER.debug(decoded);
					if(decoded!=null) {
						JsonObject decodedJson=JsonParser.parseString(decoded).getAsJsonObject();
						String name=((ServerLoginPacketListenerImpl) ctx.getNetworkManager().getPacketListener()).gameProfile.getName();
						long now=new Date().getTime();
						long dat=decodedJson.get("timeout").getAsLong();
						
						if(dat>=now&&name.equals(decodedJson.get("userName").getAsString())) {
							return;
						}
					}
				}
			}catch(Throwable t) {
				
			}
			c.get().setPacketHandled(true);
			ServerConnectionHelper.sendRedirect(ctx, ServerConnectionHelper.loginServer, true);
			ctx.getNetworkManager().disconnect(Component.translatable("message.frostedheart.not_authorized"));
			
			cbi.cancel();
		}
		
	}
}
