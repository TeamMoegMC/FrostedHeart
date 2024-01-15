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

import java.util.function.Consumer;

import com.teammoeg.frostedheart.scenario.FHScenario;
import com.teammoeg.frostedheart.scenario.parser.Scenario;
import com.teammoeg.frostedheart.scenario.runner.ScenarioConductor;

public abstract class ScenarioTarget implements IScenarioTarget{
	private final String name;
	private transient Scenario sp;
	public ScenarioTarget(String name) {
		super();
		this.name=name;
	}
	public ScenarioTarget(Scenario sc) {
		super();
		this.sp=sc;
		this.name=sc.name;
	}
	protected Scenario getScenario() {
		if(name==null)
			return null;
		if(sp==null)
			sp=FHScenario.loadScenario(name);
		return sp;
	}
	protected String getName() {
		return name;
	}
	@Override
	public void accept(ScenarioConductor runner) {
		if(name!=null&&!getScenario().equals(runner.getScenario())) {
			runner.setScenario(getScenario());
			runner.gotoNode(0);
		}
	}
}