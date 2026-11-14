/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.PolarBear;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PolarBear.class)
public abstract class PolarBearMixin extends net.minecraft.world.entity.animal.Animal implements NeutralMob {
    protected PolarBearMixin(EntityType<? extends net.minecraft.world.entity.animal.Animal> type, net.minecraft.world.level.Level level) {
        super(type, level);
    }

    @Unique private LivingEntity frostedheart$trackedTarget;
    @Unique private int frostedheart$unreachableTicks;
    @Unique private int frostedheart$fleeTicks;
    @Unique private LivingEntity frostedheart$threat;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void frostedheart$registerRetreat(CallbackInfo ci) {
        PolarBear self = (PolarBear)(Object)this;
        this.goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
            @Override public boolean canUse() { return !self.isBaby() && frostedheart$fleeTicks > 0; }
            @Override public boolean canContinueToUse() { return canUse(); }
            @Override public void start() { self.setTarget(null); self.getNavigation().stop(); }
            @Override public void tick() {
                self.setTarget(null);
                if (frostedheart$threat != null && self.getNavigation().isDone()) {
                    Vec3 away = DefaultRandomPos.getPosAway(self, 16, 7, frostedheart$threat.position());
                    if (away != null) self.getNavigation().moveTo(away.x, away.y, away.z, 1.5D);
                }
            }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void stop() { self.getNavigation().stop(); }
        });
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void frostedheart$updateCombat(CallbackInfo ci) {
        PolarBear self = (PolarBear)(Object)this;
        if (self.level().isClientSide || self.isBaby()) return;
        if (frostedheart$fleeTicks > 0) {
            --frostedheart$fleeTicks;
            self.setTarget(null);
            return;
        }
        LivingEntity attacker = self.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && self.canAttack(attacker)
                && self.tickCount - self.getLastHurtByMobTimestamp() <= 600) {
            self.setTarget(attacker);
        }
        LivingEntity target = self.getTarget();
        if (target == null || !target.isAlive()) {
            frostedheart$trackedTarget = null;
            frostedheart$unreachableTicks = 0;
            return;
        }
        if (target != frostedheart$trackedTarget) {
            frostedheart$trackedTarget = target;
            frostedheart$unreachableTicks = 0;
        }
        if (self.distanceToSqr(target) <= 6.0D + target.getBbWidth()
                && self.getSensing().hasLineOfSight(target)) frostedheart$unreachableTicks = 0;
        else ++frostedheart$unreachableTicks;
        if (self.getHealth() < self.getMaxHealth() * 0.25F || frostedheart$unreachableTicks >= 300) {
            frostedheart$threat = target;
            frostedheart$fleeTicks = 200;
            frostedheart$unreachableTicks = 0;
            self.setTarget(null);
            self.stopBeingAngry();
        }
    }

    /**
     * @author yuesha-yc
     * @reason Make bear angry in day
     */
    @Override
    public boolean isAngryAt(LivingEntity pTarget) {
        PolarBear self = (PolarBear)(Object)this;

        if (!self.isBaby() && frostedheart$fleeTicks > 0) return false;

        // Check if it's dark enough
        float lightLevel = self.getLightLevelDependentMagicValue();
        if (lightLevel > 0.5F) {
            // In day time, bear becomes angry if it can attack the target
            return self.canAttack(pTarget);
        }

        // Otherwise use the default NeutralMob behavior
        if (!this.canAttack(pTarget)) {
            return false;
        } else {
            return pTarget.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(pTarget.level())
                    ? true
                    : pTarget.getUUID().equals(this.getPersistentAngerTarget());
        }
    }

    /**
     * Injects into the createAttributes method to increase wolf stats
     */
    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private static void frostedheart$enhanceAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();

        // Modify the attributes with enhanced values
        builder.add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D);

        // Return the modified builder
        cir.setReturnValue(builder);
    }
}
