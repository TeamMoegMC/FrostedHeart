package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.network.play.ServerPlayNetHandler;

@Mixin(ServerPlayNetHandler.class)
public class MixinServerPlayNetHandler {
	@ModifyConstant(method="tick",constant=@Constant(longValue=15000))
	public long modTimeOut(long l) {
		return 30000;
	}
}
