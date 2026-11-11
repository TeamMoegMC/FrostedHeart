/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client.observation.mixin;

import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationClient;
import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationClientValidation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test input device for the opt-in client; ordinary runs never load this mixin. */
@Mixin(value = ObservationClient.class, remap = false)
public abstract class ObservationHeldInputMixin {
    @Inject(method = "held(Lcom/teammoeg/frostedresearch/knowledge/client/observation/ObservationClient$Trigger;)Z", at = @At("HEAD"), cancellable = true)
    private static void observationValidation$held(@Coerce Object trigger, CallbackInfoReturnable<Boolean> result) {
        if (Boolean.getBoolean("frostedresearch.observationValidation"))
            result.setReturnValue(trigger.toString().equals("KEY") ? ObservationClientValidation.keyHeld : ObservationClientValidation.useHeld);
    }
}
