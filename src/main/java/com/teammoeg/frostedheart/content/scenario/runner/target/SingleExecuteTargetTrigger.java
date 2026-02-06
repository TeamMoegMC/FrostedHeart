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

package com.teammoeg.frostedheart.content.scenario.runner.target;

import java.util.function.Predicate;

import com.teammoeg.frostedheart.content.scenario.runner.ScenarioThread;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioContext;

public class SingleExecuteTargetTrigger implements IScenarioTrigger {
	protected boolean canStillTrigger=true;
	protected Predicate<ScenarioContext> test;
	protected boolean async=true;

	public SingleExecuteTargetTrigger(Predicate<ScenarioContext> test) {
		super();
		this.test = test;
	}
	@Override
	public boolean test(ScenarioContext t) {

		return test.test(t);
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
	public SingleExecuteTargetTrigger setSync() {
		this.async = false;
		return this;
	}

}
