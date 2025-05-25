package com.teammoeg.frostedheart.mixin.minecraft.fix;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.FHMain;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
@Mixin(Connection.class)
public abstract class ConnectionMixin_ShowReason extends SimpleChannelInboundHandler {

    @Inject(at = @At("HEAD"), method = "exceptionCaught",require=1)
    public void fh$exceptionCaught_showreason(ChannelHandlerContext pContext, Throwable pException, CallbackInfo cbi) {
    	FHMain.LOGGER.debug("Caught exception in handling packet",pException);
    }

}
