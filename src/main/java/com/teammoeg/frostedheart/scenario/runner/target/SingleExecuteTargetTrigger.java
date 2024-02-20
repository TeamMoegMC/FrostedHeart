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

import java.util.function.Predicate;

import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.scenario.runner.IScenarioTrigger;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class SingleExecuteTargetTrigger extends ExecuteTarget implements IScenarioTrigger {
	boolean canStillTrigger=true;
	Predicate<ScenarioVM> test;
	boolean async=true;
	public SingleExecuteTargetTrigger(IScenarioThread par,String name, String label,Predicate<ScenarioVM> test) {
		super(par,name, label);
		this.test=test;
	}
	public SingleExecuteTargetTrigger(Scenario sc, String label,Predicate<ScenarioVM> test) {
		super(sc, label);
		this.test=test;
	}
	@Override
	public boolean test(ScenarioVM t) {

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
