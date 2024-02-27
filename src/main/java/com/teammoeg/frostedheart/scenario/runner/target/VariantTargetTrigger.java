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

package com.teammoeg.frostedheart.scenario.runner.target;

import com.teammoeg.frostedheart.scenario.EventTriggerType;
import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.scenario.runner.IScenarioTrigger;

import net.minecraft.entity.player.PlayerEntity;

public class VariantTargetTrigger implements IScenarioTrigger,IVarTrigger {
	boolean canStillTrigger=true;
	public boolean canTrigger;
	boolean async=true;

	@Override
	public boolean test(IScenarioThread t) {
		return canTrigger;
	}
	@Override
	public boolean use() {
		if(canStillTrigger) {
			canStillTrigger=false;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canUse() {
		return canStillTrigger;
	}
	public boolean isAsync() {
		return async;
	}
	public VariantTargetTrigger setSync() {
		this.async = false;
		return this;
	}
	@Override
	public void trigger() {
		canTrigger=true;
	}
	@Override
	public boolean canStillTrig() {
		return !canTrigger&&!canStillTrigger;
	}
	public VariantTargetTrigger register(PlayerEntity pe,EventTriggerType type) {
		FHScenario.addVarTrigger(pe, type, this);
		return this;
	}

}
