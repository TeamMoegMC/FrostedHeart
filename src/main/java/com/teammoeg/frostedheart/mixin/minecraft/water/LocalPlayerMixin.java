package com.teammoeg.frostedheart.mixin.minecraft.water;


import com.teammoeg.frostedheart.content.water.capability.WaterLevelCapability;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "hasEnoughFoodToStartSprinting()Z",at =@At("RETURN"), cancellable = true)
    public void onHasEnoughFoodToStartSprinting(CallbackInfoReturnable<Boolean> cir){
        WaterLevelCapability.getCapability((LocalPlayer)(Object)this).ifPresent(waterLevel->{
            cir.setReturnValue(cir.getReturnValue()&&waterLevel.getWaterLevel()>=6);
        });
    }
}
