package com.teammoeg.frostedheart.mixin.survive;

import com.stereowalker.survive.config.Config;
import com.stereowalker.survive.entity.SurviveEntityStats;
import com.stereowalker.survive.potion.SEffect;
import com.stereowalker.survive.potion.SEffects;
import com.stereowalker.survive.util.EnergyStats;
import com.stereowalker.survive.util.SDamageSource;
import com.stereowalker.survive.util.TemperatureStats;
import com.stereowalker.survive.util.WaterStats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SEffect.class)
public class SEffectMixin extends Effect {
    protected SEffectMixin(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }
//    @ModifyVariable(method = "performEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSleeping()Z", ordinal = 0), remap = false)
//    private boolean alwaysKillPlayerHypothermia(boolean flag) {
//        return true;
//    }
//
//    @ModifyVariable(method = "performEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSleeping()Z", ordinal = 1), remap = false)
//    private boolean alwaysKillPlayerHyperthermia(boolean flag) {
//        return true;
//    }

    /**
     * @author yuesha-yc
     * @reason This will be removed once the above code works... The logic is that hyper/hypothermia can be fatal.
     */
    @Overwrite
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
        if (this == SEffects.THIRST && entityLivingBaseIn instanceof ServerPlayerEntity) {
            WaterStats waterStats = SurviveEntityStats.getWaterStats(entityLivingBaseIn);
            waterStats.addExhaustion((PlayerEntity)entityLivingBaseIn, 0.005F * (float)(amplifier + 1));
            SurviveEntityStats.setWaterStats(entityLivingBaseIn, waterStats);
        }
        // Frosted Heart Starts
        else {
            boolean flag = (!((PlayerEntity)entityLivingBaseIn).isSleeping() || !Config.hyp_allow_sleep);
            if (this == SEffects.HYPOTHERMIA && entityLivingBaseIn instanceof PlayerEntity) {
                if (flag) {
                    if (entityLivingBaseIn.getHealth() > 10.0F) {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.5F);
                    } else if (entityLivingBaseIn.getHealth() > 5.0F) {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.3F);
                    } else {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.2F);
                    }
                }
            } else if (this == SEffects.HYPERTHERMIA && entityLivingBaseIn instanceof PlayerEntity) {
                if (flag) {
                    if (entityLivingBaseIn.getHealth() > 10.0F) {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.5F);
                    } else if (entityLivingBaseIn.getHealth() > 5.0F) {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.3F);
                    } else {
                        entityLivingBaseIn.attackEntityFrom(SDamageSource.HYPOTHERMIA, 0.2F);
                    }
                }
            }
            // Frosted Heart Ends
            else {
                TemperatureStats stats;
                if (this == SEffects.CHILLED && entityLivingBaseIn instanceof ServerPlayerEntity) {
                    stats = SurviveEntityStats.getTemperatureStats((ServerPlayerEntity)entityLivingBaseIn);
                    TemperatureStats.setTemperatureModifier(entityLivingBaseIn, "survive:chilled_effect", (double)(-(0.05F * (float)(amplifier + 1))));
                    SurviveEntityStats.setTemperatureStats((ServerPlayerEntity)entityLivingBaseIn, stats);
                } else if (this == SEffects.HEATED && entityLivingBaseIn instanceof ServerPlayerEntity) {
                    stats = SurviveEntityStats.getTemperatureStats((ServerPlayerEntity)entityLivingBaseIn);
                    TemperatureStats.setTemperatureModifier(entityLivingBaseIn, "survive:heated_effect", (double)(0.05F * (float)(amplifier + 1)));
                    SurviveEntityStats.setTemperatureStats((ServerPlayerEntity)entityLivingBaseIn, stats);
                } else {
                    EnergyStats energyStats;
                    if (this == SEffects.ENERGIZED && entityLivingBaseIn instanceof ServerPlayerEntity) {
                        energyStats = SurviveEntityStats.getEnergyStats(entityLivingBaseIn);
                        if (entityLivingBaseIn.ticksExisted % 20 == 0) {
                            energyStats.addStats(1);
                            SurviveEntityStats.setEnergyStats(entityLivingBaseIn, energyStats);
                        }

                        if (entityLivingBaseIn.isPotionActive(SEffects.TIREDNESS)) {
                            entityLivingBaseIn.removePotionEffect(SEffects.TIREDNESS);
                        }
                    } else if (this == SEffects.TIREDNESS && entityLivingBaseIn instanceof ServerPlayerEntity) {
                        energyStats = SurviveEntityStats.getEnergyStats(entityLivingBaseIn);
                        energyStats.addExhaustion((PlayerEntity)entityLivingBaseIn, 0.005F * (float)(amplifier + 1));
                        SurviveEntityStats.setEnergyStats(entityLivingBaseIn, energyStats);
                    }
                }
            }
        }

        super.performEffect(entityLivingBaseIn, amplifier);
    }
}
