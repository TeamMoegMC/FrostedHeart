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

package com.teammoeg.frostedscenario.parser;

import java.util.Map;

import com.teammoeg.frostedscenario.Param;
import com.teammoeg.frostedscenario.ScenarioUtils;
import com.teammoeg.frostedscenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedscenario.runner.target.ExecuteTarget;

public class CallNode implements Node {
	String storage;
	String label;
	String name;
	public CallNode(String command,Map<String,String> params) {
		storage=ScenarioUtils.getOfAlias(params,"s","storage");
		label=ScenarioUtils.getOfAlias(params,"l","label");
		name=ScenarioUtils.getOfAlias(params,"n","name");
				
	}
	@Override
	public String getLiteral(ScenarioCommandContext scenarioVM) {
		return "";
	}

	@Override
	public String getText() {
		return "@Call s="+storage+" l="+label+" n="+name;
	}

	@Override
	public boolean isLiteral() {
		return false;
	}

	@Override
	public void run(ScenarioCommandContext scenarioVM) {
		scenarioVM.thread().addCallStack(name==null?null:new ExecuteTarget(scenarioVM.thread().getScenario().name(),name));
		scenarioVM.thread().jump(scenarioVM.context(),storage, label);
		
	}

}
