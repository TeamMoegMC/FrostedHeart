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

package com.teammoeg.frostedheart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;

@Mixin(ClientboundUpdateRecipesPacket.class)
public abstract class ClientboundUpdateRecipesPacketMixinDebug implements Packet<ClientGamePacketListener> {
	/**
	 * @author khjxiaogu
	 * @reason debug
	 * */
	@Override
	@Overwrite
	public void handle(ClientGamePacketListener pHandler) {
		try {
			System.out.println("handling update packet");
			pHandler.handleUpdateRecipes((ClientboundUpdateRecipesPacket)(Object)this);
		}catch(Throwable t) {
			t.printStackTrace();
		}
	}

}
