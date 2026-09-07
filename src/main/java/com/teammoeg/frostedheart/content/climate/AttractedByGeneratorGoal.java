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

package com.teammoeg.frostedheart.content.climate;

import java.util.EnumSet;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
/**
 * AI Goal for animals/residents attracted by heat field
 * */
public class AttractedByGeneratorGoal extends Goal {
	private static final double SEARCH_RADIUS_BLOCKS = 32.0D;
	protected final Mob animal;
	private final double speedModifier;
	private int inAreaTicks;
	private BlockPos target;
	public AttractedByGeneratorGoal(Mob pAnimal, double pSpeedModifier) {
		this.animal = pAnimal;
		this.speedModifier = pSpeedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
	}

	/**
	 * Returns whether execution should begin. You can also read and cache any state
	 * necessary for execution in this method as well.
	 */
	public boolean canUse() {
		target = findTarget();
		return target != null;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean canContinueToUse() {
		target = findTarget();
		if (target == null) {
			return false;
		}
		if (target.distSqr(animal.blockPosition()) <= 4.0D) {
			inAreaTicks++;
			if(inAreaTicks>30) {
				inAreaTicks=0;
				return false;
			}
			return true;
		}
		return true;
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by
	 * another one
	 */
	public void stop() {
		inAreaTicks=0;
		target=null;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		if(target!=null) {
			if(target.distSqr(animal.blockPosition())<=4) {
				inAreaTicks=30;
				this.animal.getNavigation().stop();
			}else
				this.animal.getNavigation().moveTo(
						target.getX(), target.getY(), target.getZ(), this.speedModifier);
			
		}
	}

	private BlockPos findTarget() {
		return MinecraftThermalInput.nearestGameplayGenerator(
				animal.level(), animal.blockPosition(), SEARCH_RADIUS_BLOCKS);
	}

}
