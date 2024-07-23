package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Value;

@Mixin(GameRules.class)
public class GameRulesMixin{
    @Shadow
    private Map<Key<?>, Value<?>> rules;

    @Inject(method = "getBoolean", at = @At(value = "HEAD"), cancellable = true)
    public void disableWeatherCycle(GameRules.Key<GameRules.BooleanValue> key, CallbackInfoReturnable<Boolean> cir) {
        if (key == GameRules.RULE_WEATHER_CYCLE) {
            cir.setReturnValue(false);
        }
    }
    public boolean isWeatherCycle() {
        return ((BooleanValue) rules.get(GameRules.RULE_WEATHER_CYCLE)).get();
    }
}
