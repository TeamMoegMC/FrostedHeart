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

package com.teammoeg.frostedheart.content.scenario.runner.trigger;

import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioThread;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

import net.minecraft.world.entity.player.Player;

public class VariantTrigger implements IScenarioTrigger,IVarTrigger {
	boolean canStillTrigger=true;
	public boolean canTrigger;
	boolean async=true;

	@Override
	public boolean test(ScenarioContext t) {
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
	public VariantTrigger setSync() {
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
	public VariantTrigger register(Player pe,EventTriggerType type) {
		FHScenario.addVarTrigger(pe, type, this);
		return this;
	}

}
