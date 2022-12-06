package com.teammoeg.frostedheart.mixin.forge;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.play.server.SDisconnectPacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import net.minecraftforge.fml.network.FMLHandshakeMessages;
import net.minecraftforge.fml.network.NetworkEvent;

@Mixin(FMLHandshakeHandler.class)
public class FMLHandshakeAntiCheatMixin {
	@Inject(at = @At("TAIL"), method = "handleClientModListOnServer", remap = false,cancellable=true)
	void fh$handleClientModListOnServer(FMLHandshakeMessages.C2SModListReply clientModList,
			Supplier<NetworkEvent.Context> c, CallbackInfo cbi) {
		Set<String> cli=clientModList.getModList().stream().map(String::toLowerCase).collect(Collectors.toSet());
		for(String s:FHConfig.COMMON.blackmods.get()) {
			if(cli.contains(s)) {
				FHMain.LOGGER.warn("Rejected Connection: Blacklisted mods ");
				StringTextComponent t=new StringTextComponent("警告：你有被认为是作弊的mod。");
				c.get().getNetworkManager()
				.sendPacket(new SDisconnectLoginPacket(t), (p_211391_2_) -> {
					c.get().getNetworkManager().closeChannel(t);
                 });
				
				
				cbi.cancel();
			}
		}
	}
}
