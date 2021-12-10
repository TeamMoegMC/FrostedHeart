package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRules.class)
public class GameRulesMixin {
    @Inject(method = "getBoolean", at = @At(value = "HEAD"),cancellable=true)
    public void disableWeatherCycle(GameRules.RuleKey<GameRules.BooleanValue> key, CallbackInfoReturnable<Boolean> cir) {
        /*if (key == GameRules.DO_WEATHER_CYCLE) {
            cir.setReturnValue(false);
        }*/
    }
}
