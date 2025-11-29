package com.teammoeg.frostedheart.mixin.minecraft.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.Recipe;

@Mixin(ClientboundUpdateRecipesPacket.class)
public class ClientboundUpdateRecipesPacketMixin {

	public ClientboundUpdateRecipesPacketMixin() {
	}

	@Inject(at = @At("HEAD"), method = "toNetwork", require = 1)
	private static void fh$toNetwork(FriendlyByteBuf p_179470_, Recipe<?> p_179471_, CallbackInfo cbi) {
		System.out.println(p_179471_);
	}
}
