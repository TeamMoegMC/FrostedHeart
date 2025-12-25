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
