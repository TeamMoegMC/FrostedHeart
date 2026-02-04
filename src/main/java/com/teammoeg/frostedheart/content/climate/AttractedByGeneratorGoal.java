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

package com.teammoeg.frostedheart.content.climate;

import java.util.EnumSet;

import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.IHeatArea;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class AttractedByGeneratorGoal extends Goal {
	protected final Mob animal;
	private final double speedModifier;
	private int inAreaTicks;
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
		if (ChunkHeatData.hasActiveAdjust(animal.level(), animal.blockPosition())) {
			return false;
		} else if (ChunkHeatData.hasAdjust(animal.level(), animal.blockPosition())) {
			return true;
		}
		return false;
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	public boolean canContinueToUse() {
		if (ChunkHeatData.hasActiveAdjust(animal.level(), animal.blockPosition())) {
			inAreaTicks++;
			if(inAreaTicks>30) {
				inAreaTicks=0;
				return false;
			}
			return true;
		} else if (ChunkHeatData.hasAdjust(animal.level(), animal.blockPosition())) {
			return true;
		}
		return false;
	}

	/**
	 * Reset the task's internal state. Called when this task is interrupted by
	 * another one
	 */
	public void stop() {
		inAreaTicks=0;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	public void tick() {
		IHeatArea adjust=ChunkHeatData.getNearestAdjust(animal.level(), animal.blockPosition());
		if(adjust!=null) {
			BlockPos pos = adjust.getCenter();
			if(pos.distSqr(animal.blockPosition())<=4) {
				inAreaTicks=30;
				this.animal.getNavigation().stop();
			}else
				this.animal.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), adjust.isEffective(animal.blockPosition())?this.speedModifier*0.5:this.speedModifier);
			
		}
	}

}
