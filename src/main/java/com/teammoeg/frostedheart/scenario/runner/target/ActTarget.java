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

import com.teammoeg.frostedheart.scenario.runner.ActNamespace;
import com.teammoeg.frostedheart.scenario.runner.IScenarioThread;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;
import com.teammoeg.frostedheart.scenario.runner.ScenarioVM;

public class ActTarget implements IScenarioTarget {
	ActNamespace ns;
	IScenarioTarget parent;
	public ActTarget(ActNamespace ns, IScenarioTarget parent) {
		super();
		this.ns = ns;
		this.parent = parent;
	}
	@Override
	public void accept(ScenarioVM t) {
		if(t instanceof ScenarioConductor)
			((ScenarioConductor) t).continueAct(ns);
		parent.apply(t);
		
	}
	@Override
	public void apply(IScenarioThread conductor) {
		parent.apply(conductor);
	}

}
