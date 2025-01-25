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

package com.teammoeg.frostedheart.content.scenario.parser;

import java.util.Map;

import com.teammoeg.frostedheart.content.scenario.Param;
import com.teammoeg.frostedheart.content.scenario.ScenarioUtils;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioCommandContext;
import com.teammoeg.frostedheart.content.scenario.runner.target.ExecuteTarget;

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
