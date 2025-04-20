package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net.minecraft.world.entity.animal.PolarBear$PolarBearMeleeAttackGoal")
public abstract class PolarBearMeleeAttackGoalMixin extends MeleeAttackGoal {
    public PolarBearMeleeAttackGoalMixin(PathfinderMob mob, double speed, boolean longMemory) {
        super(mob, speed, longMemory);
    }

    /**
     * @author yuesha-yc
     * @reason increase attack range
     */
    @Overwrite
    protected double getAttackReachSqr(LivingEntity target) {
        // bump 4.0F → 6.0F (or whatever)
        return 6.0F + target.getBbWidth();
    }
}

