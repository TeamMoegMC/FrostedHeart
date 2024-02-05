package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanValue;
import net.minecraft.world.GameRules.RuleKey;
import net.minecraft.world.GameRules.RuleValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(GameRules.class)
public class GameRulesMixin{
    @Shadow
    private Map<RuleKey<?>, RuleValue<?>> rules;

    @Inject(method = "getBoolean", at = @At(value = "HEAD"), cancellable = true)
    public void disableWeatherCycle(GameRules.RuleKey<GameRules.BooleanValue> key, CallbackInfoReturnable<Boolean> cir) {
        if (key == GameRules.DO_WEATHER_CYCLE) {
            cir.setReturnValue(false);
        }
    }
    public boolean isWeatherCycle() {
        return ((BooleanValue) rules.get(GameRules.DO_WEATHER_CYCLE)).get();
    }
}
