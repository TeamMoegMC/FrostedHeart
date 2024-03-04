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

package com.teammoeg.frostedheart.content.scenario.runner.target;

import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.parser.Scenario;
import com.teammoeg.frostedheart.content.scenario.runner.IScenarioThread;

public abstract class ScenarioTarget implements IScenarioTarget{
	private final String name;
	private transient Scenario sp;
	private transient IScenarioThread cd;
	public ScenarioTarget(IScenarioThread cdr,String name) {
		super();
		this.name=name;
		cd=cdr;
		if(name==null) {
			sp=cdr.getScenario();
			if(sp!=null)
				name=sp.name;
		}
	}
	public ScenarioTarget(Scenario sc) {
		super();
		this.sp=sc;
		this.name=sc.name;
	}
	public Scenario getScenario() {
		if(name==null)
			return null;
		if(sp==null)
			sp=FHScenario.loadScenario(cd,name);
		return sp;
	}
	@Override
	public void apply(IScenarioThread conductor) {
		if(name!=null&&!getScenario().equals(conductor.getScenario())) {
			conductor.setScenario(getScenario());
			conductor.setNodeNum(0);
		}
	}
	protected String getName() {
		return name;
	}
	@Override
	public String toString() {
		return "[name=" + name + "]";
	}

}