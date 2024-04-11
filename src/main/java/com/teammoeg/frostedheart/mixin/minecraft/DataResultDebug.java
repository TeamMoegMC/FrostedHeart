package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.Lifecycle;
@Mixin(DataResult.class)
public class DataResultDebug {
    @Inject(at = @At("RETURN"), method = "<init>(Lcom/mojang/datafixers/util/Either;Lcom/mojang/serialization/Lifecycle;)V",remap=false)
    private void fh$init(Either result,Lifecycle lifecycle,CallbackInfo ci){
    	result.ifRight(p->{
    		System.out.println(((PartialResult)p).message());
        	new Exception().printStackTrace();
    	});
    
    }
}
