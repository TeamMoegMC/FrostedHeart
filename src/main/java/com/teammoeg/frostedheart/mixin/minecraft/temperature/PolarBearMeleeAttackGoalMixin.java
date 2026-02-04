/*
 * Copyright (c) 2024 TeamMoeg
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

