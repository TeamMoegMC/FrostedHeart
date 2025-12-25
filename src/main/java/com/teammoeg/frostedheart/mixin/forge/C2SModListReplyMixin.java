package com.teammoeg.frostedheart.mixin.forge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.clusterserver.ClientConnectionHelper;
import com.teammoeg.frostedheart.clusterserver.ServerConnectionHelper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.HandshakeMessages.C2SModListReply;

@Mixin(C2SModListReply.class)
public class C2SModListReplyMixin {

	public C2SModListReplyMixin() {

	}
	@Inject(at=@At("HEAD"),method="decode",remap=false)
    private static void FH$decode(FriendlyByteBuf input,CallbackInfoReturnable<C2SModListReply> info)
    {
		if(input.readBoolean())
			ServerConnectionHelper.currentToken.set(input.readUtf());
		else
			ServerConnectionHelper.currentToken.set(null);
    }
	@Inject(at=@At("HEAD"),method="encode",remap=false)
	private void encode(FriendlyByteBuf output,CallbackInfo cbi)
    {
		if(ClientConnectionHelper.token!=null) {
			output.writeBoolean(true);
			output.writeUtf(ClientConnectionHelper.token);
		}else {
			output.writeBoolean(false);
		}
    }
}
