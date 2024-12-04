package com.teammoeg.frostedheart.mixin.ftb;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.renderer.ShaderInstance;

@Mixin(RenderSystem.class)
public class IconMixin {
	@Inject(at=@At("HEAD"),method="setShader",remap=false,cancellable=true)
	private static void setShader(Supplier<ShaderInstance> shader,CallbackInfo cbi) {
		if(shader.get()==null)
		new Exception().printStackTrace();
		//cbi.setReturnValue(JsonNull.INSTANCE);
	}

}
