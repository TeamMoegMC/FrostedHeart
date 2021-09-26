package com.teammoeg.frostedheart.climate;

import com.teammoeg.frostedheart.FHEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class HypothermiaEffect extends Effect {
    public HypothermiaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
        if (entityLivingBaseIn instanceof ServerPlayerEntity) {
            if (entityLivingBaseIn.getHealth() > 10.0F) {
                entityLivingBaseIn.attackEntityFrom(FHEffects.FHDamageSources.HYPOTHERMIA, 0.5F);
            } else if (entityLivingBaseIn.getHealth() > 5.0F) {
                entityLivingBaseIn.attackEntityFrom(FHEffects.FHDamageSources.HYPOTHERMIA, 0.3F);
            } else {
                entityLivingBaseIn.attackEntityFrom(FHEffects.FHDamageSources.HYPOTHERMIA, 0.2F);
            }
        }
    }

    public boolean isReady(int duration, int amplifier) {
        int k = 40 >> amplifier;
        if (k > 0) {
            return duration % k == 0;
        } else {
            return true;
        }
    }
}
